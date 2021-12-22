/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties.prop
import no.ndla.network.model.HttpRequestException
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.joda.time.DateTime
import org.json4s
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalaj.http.{Http, HttpRequest, HttpResponse}
import scalikejdbc.{DB, DBSession, _}

import java.util.concurrent.Executors
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class BrightcoveToken(access_token: String, expires_in: Long)
case class StoredToken(accessToken: String, expiresAt: Long)
case class BrightcoveData(id: String)

class BrightcoveApiClient {
  implicit val jsonFormats: Formats = DefaultFormats

  private lazy val BrightcoveAccountId = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  private lazy val ClientId = prop("BRIGHTCOVE_API_CLIENT_ID")
  private lazy val ClientSecret = prop("BRIGHTCOVE_API_CLIENT_SECRET")
  private val timeout = 20 * 1000 // 20 Seconds

  private var accessToken: Option[StoredToken] = None

  def fetchBrightcoveVideo(id: String): Try[BrightcoveData] = {
    refreshTokenIfInvalid().flatMap(token => fetch(id, token))
  }

  private def refreshToken(): Try[StoredToken] = {
    fetchAccessToken().map(token => {
      val temp = StoredToken(
        token.access_token,
        token.expires_in + (new DateTime().getMillis / 1000)
      )
      accessToken = Some(temp)
      temp
    })
  }

  def refreshTokenIfInvalid(): Try[StoredToken] = {
    accessToken match {
      case Some(storedToken) if (new DateTime().getMillis / 1000) < storedToken.expiresAt - 10 => Success(storedToken)
      case _                                                                                   => refreshToken()
    }
  }

  def fetch(id: String, token: StoredToken): Try[BrightcoveData] = {
    doRequest(
      Http(s"https://cms.api.brightcove.com/v1/accounts/$BrightcoveAccountId/videos/$id")
        .header("Authorization", s"Bearer ${token.accessToken}")
        .timeout(timeout, timeout))
      .flatMap(e => extract[BrightcoveData](e.body))
  }

  private def fetchAccessToken(): Try[BrightcoveToken] = {
    doRequest(
      Http("https://oauth.brightcove.com/v4/access_token?grant_type=client_credentials")
        .method("POST")
        .auth(ClientId, ClientSecret)
        .timeout(timeout, timeout))
      .flatMap(e => extract[BrightcoveToken](e.body))
  }

  private def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): Try[T] = {
    Try { read[T](json) } match {
      case Failure(e)    => Failure(new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage))))
      case Success(data) => Success(data)
    }
  }

  private def doRequest(request: HttpRequest): Try[HttpResponse[String]] = {
    Try(request.asString).flatMap(response => {
      if (response.isError) {
        Failure(new HttpRequestException(
          s"Received error ${response.code} ${response.statusLine} when calling ${request.url}. Body was ${response.body}",
          Some(response)))
      } else {
        Success(response)
      }
    })
  }
}

class V31__ConvertBrightcoveIds extends BaseJavaMigration with LazyLogging {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  val Brightcove = new BrightcoveApiClient

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

    while (numPagesLeft > 0) {
      var futures = ListBuffer.empty[Future[(String, Long)]]

      allArticles(offset * 1000).map {
        case (id, document) =>
          // Convert each article in separate thread
          futures += Future { convertArticleUpdate(document, id) }
      }

      numPagesLeft -= 1
      offset += 1

      // Wait for all threads to finish before doing database stuff because memory runs out if we do all at once
      val futs = Future.sequence(futures)
      val allThemArticles = Await.result(futs, Duration.Inf)

      allThemArticles.map {
        case (newDocument, articleId) =>
          updateArticle(newDocument, articleId)(session)
      }
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

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  private def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  def updateContent(html: String, id: Long): String = {
    val doc = stringToJsoupDocument(html)
    doc
      .select("embed")
      .forEach(embed => {
        val dataResource = embed.attr("data-resource")
        if (dataResource == "brightcove") {
          val brightcoveId = embed.attr("data-videoid")
          if (brightcoveId.contains("ref:")) {
            Brightcove.fetchBrightcoveVideo(brightcoveId) match {
              case Success(brightcoveObj) =>
                println(s"Successfully fetched ${brightcoveId} as ${brightcoveObj.id} for article $id")
                embed.attr("data-videoid", brightcoveObj.id)
              case Failure(exception) =>
                println(s"Article with id $id failed to update BrightcoveId. ${exception.getMessage}")
            }
          }
        }
      })
    jsoupDocumentToString(doc)
  }

  def updateContent(contents: JArray, contentType: String, id: Long): json4s.JValue = {
    contents.map(content =>
      content.mapField {
        case (`contentType`, JString(html)) => (`contentType`, JString(updateContent(html, id)))
        case z                              => z
    })
  }

  private[migration] def convertArticleUpdate(document: String, id: Long): (String, Long) = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("visualElement", visualElements: JArray) => {
        val updatedContent = updateContent(visualElements, "resource", id)
        ("visualElement", updatedContent)
      }
      case ("content", contents: JArray) => {
        val updatedContent = updateContent(contents, "content", id)
        ("content", updatedContent)
      }
      case x => x
    }

    compact(render(newArticle)) -> id
  }

}
