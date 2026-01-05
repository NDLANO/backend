/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import io.circe.{ACursor, Json}
import io.lemonlabs.uri.UrlPath
import no.ndla.common.CirceUtil
import no.ndla.common.aws.NdlaS3Client
import no.ndla.database.DocumentMigration

import scala.collection.concurrent.TrieMap

extension (cursor: ACursor) {
  def expectTop: Json = cursor.top.getOrElse(throw RuntimeException("Expected top Json value"))
}

class V25__FixUrlEncodedFileNames(using s3Client: NdlaS3Client) extends DocumentMigration with StrictLogging {
  override val tableName: String                    = "imagemetadata"
  override val columnName: String                   = "metadata"
  private val existsCache: TrieMap[String, Boolean] = TrieMap.empty

  private def objectExists(key: String): Boolean = existsCache.getOrElseUpdate(key, s3Client.objectExists(key))

  private def updateImageFileName(imageJson: Json): Json = {
    val cursor   = imageJson.hcursor
    val fileName = cursor.get[String]("fileName") match {
      case Left(_)                                   => throw new RuntimeException("Missing fileName for image file")
      case Right(fileName) if objectExists(fileName) => return imageJson
      case Right(fileName)                           => fileName
    }

    val decodedFileName = UrlPath.parse(fileName).toStringRaw
    if (objectExists(decodedFileName)) {
      cursor.withFocus(_.mapObject(_.add("fileName", Json.fromString(decodedFileName)))).expectTop
    } else {
      logger.warn(
        s"Could not find S3 object for neither fileName or URL decoded fileName. Original fileName was: $fileName"
      )
      imageJson
    }
  }

  override def convertColumn(value: String): String = {
    val document = CirceUtil.unsafeParse(value)
    val images   = document.hcursor.downField("images")

    if (images.succeeded) {
      images.withFocus(_.mapArray(_.map(updateImageFileName))).expectTop.noSpaces
    } else {
      document.noSpaces
    }
  }
}
