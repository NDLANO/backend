/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.DraftApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V20__UpdateH5PDomainForFF extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    if (DraftApiProperties.Environment == "ff") {
      db.withinTx { implicit session =>
        migrateArticles
      }
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def allArticlesWithArticleId(articleId: Long)(implicit session: DBSession) = {
    sql"select id, document from articledata where document is not null and article_id=${articleId} order by id"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
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

  def updateH5PDomains(html: String): String = {
    val oldDomain = "h5p.ndla.no"
    val newDomain = "h5p-ff.ndla.no"

    html.replaceAll(oldDomain, newDomain)
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("content", contents: JArray) =>
        val updatedContent = contents.map {
          case content: JObject =>
            content.mapField {
              case ("content", JString(html)) => ("content", JString(updateH5PDomains(html)))
              case z                          => z
            }
          case y => y
        }
        ("content", updatedContent)
      case x => x
    }

    compact(render(newArticle))
  }
}
case class V19__Article(content: Seq[V19__Content])
case class V19__Content(content: String, language: String)
