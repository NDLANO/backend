/*
 * Part of NDLA image-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3Object}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.Uri
import no.ndla.imageapi.ImageApiProperties.StorageName
import org.apache.commons.io.IOUtils
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JField, JInt}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class V12__AddSizeMetaData extends BaseJavaMigration with LazyLogging {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    val NumThreads                                          = 10
    val executorService                                     = Executors.newWorkStealingPool(NumThreads)
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)

    db.withinTx { implicit session =>
      val toUpdate      = imagesToUpdate(session)
      val totalToUpdate = toUpdate.size
      val futs = toUpdate.zipWithIndex.map { case ((id, document), idx) =>
        val f = Future { convertImageUpdate(document) }

        f.onComplete {
          case Success(converted) =>
            update(converted, id)
            println(s"Completed convertion of: ${idx + 1}/$totalToUpdate")
          case Failure(ex) => println(("NO", ex))
        }

        f
      }
      val mergedFuture = Future.sequence(futs)
      while (!mergedFuture.isCompleted) {
        Thread.sleep(1000)
      }
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }

  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_CENTRAL_1))
      .build()

  def getS3Object(imageKey: String): Try[S3Object] = {
    Try(amazonClient.getObject(new GetObjectRequest(StorageName, imageKey)))
  }

  def get(imageKey: String): Try[Option[(Int, Int)]] = {
    getS3Object(imageKey).flatMap(s3Object => {
      val s3Is         = s3Object.getObjectContent
      val imageContent = IOUtils.toByteArray(s3Is)
      val stream       = new ByteArrayInputStream(imageContent)
      val image        = Try(Option(ImageIO.read(stream)))

      image match {
        case Success(Some(image)) => Success(Some(image.getWidth -> image.getHeight))
        case Success(None) =>
          val isSVG = new String(imageContent).toLowerCase.contains("<svg")
          if (isSVG) {
            // Since SVG are vector-images size doesn't make sense
            Success(None)
          } else {
            Failure(new RuntimeException("Broken image"))
          }
        case Failure(ex) => Failure(ex)
      }
    })
  }

  def convertImageUpdate(imageMeta: String): String = {
    val oldDocument = parse(imageMeta)

    val imageUrl = (oldDocument \ "imageUrl").extract[String]
    val imageKey = Uri
      .parse(imageUrl)
      .toStringRaw
      .dropWhile(_ == '/') // Strip heading '/'
    val newDocument = get(imageKey) match {
      case Success(Some((width, height))) =>
        val toMerge = JObject(
          JField(
            "imageDimensions",
            JObject(
              JField("width", JInt(width)),
              JField("height", JInt(height))
            )
          )
        )
        oldDocument.merge(toMerge)
      case Success(None) =>
        oldDocument
      case Failure(ex) =>
        ex match {
          case aex: AmazonS3Exception if aex.getStatusCode == 404 =>
            logger.warn(s"$imageKey was not found on s3.")
          case ex =>
            logger.warn(s"Something went wrong when fetching $imageKey", ex)
        }

        val toMerge = JObject(JField("imageDimensions", JObject(JField("width", JInt(0)), JField("height", JInt(0)))))
        oldDocument.merge(toMerge)
    }

    compact(render(newDocument))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update()
  }
}
