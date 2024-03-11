/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.common.model.domain.ArticleType
import no.ndla.draftapi.{DraftApiProperties, Props}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.*
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DBSession, _}
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.Try

class R__SetArticleTypeFromTaxonomy(properties: DraftApiProperties) extends BaseJavaMigration with Props {
  override val props: DraftApiProperties = properties

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  private val TaxonomyApiEndpoint           = s"${props.Domain}/taxonomy/v1"
  private val taxonomyTimeout               = 20.seconds

  case class TaxonomyResource(contentUri: Option[String])

  override def getChecksum: Integer = 0 // Change this to something else if you want to repeat migration

  def fetchResourceFromTaxonomy(endpoint: String): Seq[Long] = {
    val url = TaxonomyApiEndpoint + endpoint

    val resourceList = for {
      response  <- Try(simpleHttpClient.send(quickRequest.get(uri"$url").readTimeout(taxonomyTimeout)))
      extracted <- Try(parse(response.body).extract[Seq[TaxonomyResource]])
    } yield extracted

    resourceList
      .getOrElse(Seq.empty)
      .flatMap(resource =>
        resource.contentUri.flatMap(contentUri => {
          val splits    = contentUri.split(':')
          val articleId = splits.lastOption.filter(_ => splits.contains("article"))
          articleId.flatMap(idStr => Try(idStr.toLong).toOption)
        })
      )
  }

  override def migrate(context: Context): Unit = {
    /*
    // Environments are migrated already
    // So this is a noop migration to speed up tests and fresh local runs
    // If we want to repeat migration just remove this comment and change checksum

    DB(context.getConnection)
      .autoClose(false)
      .withinTx { implicit session =>
        migrateArticles
      }
     */
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    val topicIds: Seq[Long]    = fetchResourceFromTaxonomy("/topics")
    val resourceIds: Seq[Long] = fetchResourceFromTaxonomy("/resources")

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).foreach { case (id, articleId, document) =>
        updateArticle(convertArticleUpdate(document, articleId, topicIds, resourceIds), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def convertArticleUpdate(document: String, id: Long, topicIds: Seq[Long], resourceIds: Seq[Long]): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("articleType", _: JString) if topicIds.contains(id) =>
        "articleType" -> JString(ArticleType.TopicArticle.entryName)
      case ("articleType", _: JString) if resourceIds.contains(id) && !topicIds.contains(id) =>
        "articleType" -> JString(ArticleType.Standard.entryName)
      case x => x
    }
    compact(render(newArticle))
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, Long, String)] = {
    sql"select id, article_id, document from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.long("article_id"), rs.string("document"))
      })
      .list()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

}
