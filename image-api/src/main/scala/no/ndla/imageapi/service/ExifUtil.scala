/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.sksamuel.scrimage.metadata.ImageMetadata
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.UploadedFile

import java.io.InputStream
import scala.util.{Try, Using}

object ExifUtil extends StrictLogging {
  // Filter out directories that often contain large binary data or irrelevant metadata
  private val unwantedExifDirectories = Seq("ICC Profile", "Photoshop", "PNG-tEXt")
  // Common EXIF date/time tags to check for when extracting the original capture date
  private val ExifDateTimeOriginal = "Exif SubIFD:Date/Time Original"
  private val ExifDateTime         = "Exif IFD0:Date/Time"

  def extractDate(exifData: Option[Map[String, String]]): Option[String] =
    exifData.flatMap(data => data.get(ExifDateTimeOriginal).orElse(data.get(ExifDateTime)))

  /** Sanitizes a string so it is safe for JSON serialization by removing non-printable control characters and replacing
    * invalid unicode characters with the unicode replacement character.
    */
  private def sanitizeForJson(value: String): String = {
    value.map { ch =>
      if (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t') '\uFFFD'
      else if (Character.isHighSurrogate(ch) || Character.isLowSurrogate(ch)) '\uFFFD'
      else ch
    }
  }

  private def extractMetadataMap(metadata: ImageMetadata): Map[String, String] = {
    metadata
      .getDirectories
      .filter(d => !unwantedExifDirectories.contains(d.getName))
      .flatMap { directory =>
        directory
          .getTags
          .flatMap { tag =>
            val name  = s"${directory.getName}:${tag.getName}"
            val value = tag.getRawValue
            Option.when(value != null && value.nonEmpty)(name -> sanitizeForJson(value))
          }
      }
      .toMap
  }

  /** Extracts all EXIF key-value pairs from an uploaded image file. Returns an empty map if no EXIF data is found or if
    * reading fails.
    */
  def extractExifData(file: UploadedFile): Map[String, String] = {
    Using(file.createStream()) { stream =>
      extractExifDataFromStream(stream)
    }.recover { case ex =>
        logger.warn(s"Failed to extract EXIF data from uploaded image: ${ex.getMessage}", ex)
        Map.empty[String, String]
      }
      .getOrElse(Map.empty)
  }

  /** Extracts all EXIF key-value pairs from an InputStream. Returns an empty map if no EXIF data is found or if reading
    * fails.
    */
  def extractExifDataFromStream(stream: InputStream): Map[String, String] = {
    Try {
      extractMetadataMap(ImageMetadata.fromStream(stream))
    }.recover { case ex =>
        logger.warn(s"Failed to extract EXIF data from stream: ${ex.getMessage}", ex)
        Map.empty[String, String]
      }
      .getOrElse(Map.empty)
  }
}
