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

import java.io.InputStream
import scala.util.Try

object ExifUtil extends StrictLogging {

  // Common EXIF date/time tags to check for when extracting the original capture date
  private val ExifDateTimeOriginal = "Exif SubIFD:Date/Time Original"
  private val ExifDateTime         = "Exif IFD0:Date/Time"

  def extractDate(exifData: Option[Map[String, String]]): Option[String] =
    if (exifData.isEmpty) {
      None
    } else {
      exifData.flatMap(data => data.get(ExifDateTimeOriginal).orElse(data.get(ExifDateTime)).orElse(None))
    }

  /** Extracts all EXIF key-value pairs from an ImageMetadata.
    */
  def extractMetadataMap(metadata: ImageMetadata): Option[Map[String, String]] = {
    val values = metadata
      .getDirectories
      .flatMap { directory =>
        directory
          .getTags
          .flatMap { tag =>
            val name  = s"${directory.getName}:${tag.getName}"
            val value = tag.getRawValue
            Option.when(value != null && value.nonEmpty)(name -> value)
          }
      }
      .toMap
    if (values.isEmpty) None
    else Some(values)
  }

  /** Extracts all EXIF key-value pairs from an InputStream.
    */
  def extractExifDataFromStream(stream: InputStream): Option[Map[String, String]] = {
    Try {
      extractMetadataMap(ImageMetadata.fromStream(stream))
    }.recover { case ex =>
        logger.warn(s"Failed to extract EXIF data from stream: ${ex.getMessage}", ex)
        None
      }
      .getOrElse(None)
  }
}
