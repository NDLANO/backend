/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.model.domain.ArticleType
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JArray
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.util.{Success, Try}

class V15__RemoveVisualElementFromStandard extends BaseJavaMigration {
  implicit val formats = org.json4s.DefaultFormats + new EnumNameSerializer(ArticleType)

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  private def migrateArticles(implicit session: DBSession): Unit = {
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

  private def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from articledata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    Try(oldArticle.extract[V15__Article]) match {
      case Success(old) =>
        val newArticle = old.articleType match {
          case ArticleType.Standard =>
            oldArticle.mapField {
              case ("visualElement", _: JArray) => "visualElement" -> JArray(List.empty)
              case x                            => x
            }
          case ArticleType.TopicArticle => oldArticle
        }
        compact(render(newArticle))
      case _ => compact(render(oldArticle))
    }
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = ${dataObject} where id = ${id}"
      .update()
  }

  case class V15__Article(articleType: ArticleType.Value)
}
