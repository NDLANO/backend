/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.Tag
import no.ndla.database.TableMigration
import no.ndla.network.clients.TaxonomyApiClient
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.Implicits.scalikejdbcSQLInterpolationImplicitDef
import scalikejdbc.{DBSession, SQLSyntax, WrappedResultSet}

case class DocumentRow(id: Long, document: String, article_id: Long)

class V64__SetResourceTypeFromTaxonomyAsTag()(using taxonomyClient: TaxonomyApiClient)
    extends TableMigration[DocumentRow] {

  override val tableName: String            = "contentdata"
  private val columnName                    = "document"
  private lazy val columnNameSQL: SQLSyntax = SQLSyntax.createUnsafely(columnName)
  override lazy val whereClause: SQLSyntax  = sqls"$columnNameSQL is not null"

  private lazy val taxonomyBundle = taxonomyClient.getTaxonomyBundleUncached(true).get

  override def extractRowData(rs: WrappedResultSet): DocumentRow =
    DocumentRow(rs.long("id"), rs.string(columnName), rs.long("article_id"))

  def updateRow(rowData: DocumentRow)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    val newDocument = convertColumn(rowData.article_id, rowData.document)
    dataObject.setValue(newDocument)
    sql"""update $tableNameSQL
          set $columnNameSQL = $dataObject
          where id = ${rowData.id}
       """
      .update()
  }

  def convertColumn(articleId: Long, document: String): String = {
    val node = taxonomyBundle.nodeByContentUri.get(s"urn:article:$articleId") match {
      case Some(n) => n.headOption
      case None    => return document
    }
    if node.isEmpty then return document

    val resourceTypes =
      node
        .flatMap(n => n.context.map(c => c.resourceTypes.filter(rt => rt.parentId.isDefined).map(rt => rt.name)))
        .getOrElse(List.empty)

    val oldDocument = parser.parse(document).toTry.get
    val tags        = oldDocument.hcursor.downField("tags").as[Option[Seq[Tag]]].toTry.get.getOrElse(Seq.empty)
    // Insert values from searchablelanguagevalues as tags if they are not already present
    val uniqueTagsByLang = tags.foldLeft(Seq.empty[(String, Set[String])]) { (acc, tag) =>
      acc :+ (tag.language, Set.from(tag.tags))
    }
    val newTags = uniqueTagsByLang.map((tag) => {
      val newTags =
        resourceTypes.flatMap(rt => rt.languageValues.collect { case lng if lng.language == tag._1 => lng.value })
      Tag(language = tag._1, tags = tag._2.concat(newTags).toList)
    })
    val updatedDocument = oldDocument.hcursor.downField("tags").withFocus(_ => newTags.asJson)
    updatedDocument.top.get.noSpaces
  }
}
