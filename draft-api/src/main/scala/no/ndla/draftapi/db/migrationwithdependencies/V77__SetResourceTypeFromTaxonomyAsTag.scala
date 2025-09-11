/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.Tag
import no.ndla.database.TableMigration
import no.ndla.network.clients.TaxonomyApiClient
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.Implicits.scalikejdbcSQLInterpolationImplicitDef
import scalikejdbc.{DBSession, SQLSyntax, WrappedResultSet}

case class DocumentRow(id: Long, document: String, article_id: Long)

class V77__SetResourceTypeFromTaxonomyAsTag()(using taxonomyClient: TaxonomyApiClient)
    extends TableMigration[DocumentRow] {
  override val tableName: String            = "articledata"
  private val columnName                    = "document"
  private lazy val columnNameSQL: SQLSyntax = SQLSyntax.createUnsafely(columnName)
  override lazy val whereClause: SQLSyntax  = sqls"$columnNameSQL is not null"

  private val taxonomyBundle = taxonomyClient.getTaxonomyBundleUncached(true).get

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
    // A Tag has a language and a sequence of strings. A SearchableLanguageValues has a language and a single string
    // We add the value from searchablelanguagevalues to the tags if it is not already present in the tags for the language from tag
    val newTags = resourceTypes.iterator.foldLeft(tags) { (acc, rt) =>
      val languages = acc.map(_.language).distinct
      languages.foldLeft(acc) { (innerAcc, lang) =>
        val existingTag = innerAcc.find(t => t.language == lang)
        existingTag match {
          case Some(_) =>
            innerAcc.find(t => t.language == lang) match {
              case Some(tag) =>
                val updatedTags =
                  tag.tags :+ rt.languageValues.find(lv => lv.language == lang).map(_.value).getOrElse("")
                innerAcc.map(t => if (t.language == lang) t.copy(tags = updatedTags.distinct) else t)
              case None => innerAcc
            }
          case _ => innerAcc
        }
      }
    }
    val updatedDocument = oldDocument.hcursor.downField("tags").withFocus(_ => newTags.asJson)
    updatedDocument.top.get.noSpaces
  }
}
