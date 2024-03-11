/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.learningpathapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.Url
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.InvalidOembedResponse
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.*
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}
import sttp.client3.quick._

import java.util.concurrent.Executors
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class V13__StoreNDLAStepsAsIframeTypes(props: LearningpathApiProperties) extends BaseJavaMigration with StrictLogging {
  import props.{ApiGatewayHost, NdlaFrontendHost, NdlaFrontendProtocol}
  implicit val formats: Formats = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session => migrateLearningSteps }

  def migrateLearningSteps(implicit session: DBSession): Unit = {

    val count        = countAllLearningSteps.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(3))

    val futs = mutable.ListBuffer[Future[Unit]]()

    while (numPagesLeft > 0) {
      allLearningSteps(offset * 1000).foreach { case (id, document) =>
        futs += Future {
          updateLearningStep(migrateLearningStepDocument(document), id)(session): Unit
        }
      }
      numPagesLeft -= 1
      offset += 1
    }

    val futureCollection = Future.sequence(futs)

    futureCollection.onComplete {
      case Success(_)  => logger.info("V13 migration finished successfully")
      case Failure(ex) => logger.error("V13 migration failed!", ex)
    }

    Await.ready(futureCollection, Duration.Inf)
  }

  def countAllLearningSteps(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from learningsteps where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allLearningSteps(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningsteps where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateLearningStep(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningsteps set document = $dataObject where id = $id"
      .update()
  }

  def updateNdlaUrl(oldUrl: String): String = {
    val parsed     = Url.parse(oldUrl)
    val isNDLAHost = parsed.hostOption.exists(_.toString.contains("ndla.no"))

    if (isNDLAHost) parsed.toRelativeUrl.toString
    else oldUrl
  }

  case class OembedResponse(html: String)

  def getUrlFromIframe(html: String): Try[String] = {
    val soup = Jsoup.parse(html)
    val elem = Option(soup.selectFirst("iframe"))
    Option(elem.map(_.attr("src")).filterNot(_.isEmpty)).flatten match {
      case Some(url) => Success(url)
      case None =>
        logger.error(s"could not parse url in html '$html'")
        Failure(InvalidOembedResponse(s"could not parse url in html '$html'"))
    }
  }

  def getNewUrl(url: String): Try[String] = {
    val request  = quickRequest.get(uri"http://$ApiGatewayHost/oembed-proxy/v1/oembed?url=$url")
    val response = simpleHttpClient.send(request)
    response match {
      case resp if !resp.isSuccess =>
        logger.error(s"Could not fetch oembed for $url -> ${resp.code}:\n\t${resp.body}")
        Failure(new RuntimeException(s"Failed to fetch oembed for $url"))
      case response =>
        val oembed = read[OembedResponse](response.body)
        getUrlFromIframe(oembed.html)
    }
  }

  private[migrationwithdependencies] def migrateLearningStepDocument(document: String): String = {
    val oldLs = parse(document)

    val embedUrlObjects = (oldLs \ "embedUrl").extract[JArray]
    val newObjects = embedUrlObjects.arr.map(embedUrlObject => {
      (embedUrlObject \ "url").extractOpt[String] match {
        case Some(url) if url.nonEmpty =>
          val parsed        = Url.parse(url)
          val isRelativeUrl = parsed.hostOption.isEmpty
          if (!isRelativeUrl) { embedUrlObject }
          else {
            val h = s"$NdlaFrontendProtocol://$NdlaFrontendHost$url"
            getNewUrl(h) match {
              case Failure(_) => embedUrlObject
              case Success(newUrl) =>
                val newRelative = Url.parse(newUrl).toRelativeUrl.toString
                embedUrlObject.mapField {
                  case ("embedType", JString("oembed")) => ("embedType", JString("iframe"))
                  case ("url", JString(_))              => ("url", JString(newRelative))
                  case x                                => x
                }
            }
          }
        case _ => embedUrlObject
      }
    })

    val newLs = oldLs.replace(List("embedUrl"), JArray(newObjects))
    compact(render(newLs))
  }
}
