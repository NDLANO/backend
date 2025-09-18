/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.grep

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.search.LanguageValue
import no.ndla.mapping.ISO639

case class GrepTitle(spraak: String, verdi: String)

object GrepTitle extends StrictLogging {
  implicit val encoder: Encoder[GrepTitle] = deriveEncoder
  implicit val decoder: Decoder[GrepTitle] = deriveDecoder

  def convertTitles(titles: Seq[GrepTitle]): Seq[LanguageValue[String]] = {
    titles.flatMap(gt => {
      ISO639.get6391CodeFor6392Code(gt.spraak) match {
        case Some(convertedLanguage) =>
          Some(LanguageValue(language = convertedLanguage, value = gt.verdi.trim))
        case None if gt.spraak == "default" => None
        case None                           =>
          logger.warn(s"Could not convert language code '${gt.spraak}'")
          None
      }
    })
  }
}
