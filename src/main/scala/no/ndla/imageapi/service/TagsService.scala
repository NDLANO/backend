/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.io.InputStream
import java.net.URL

import no.ndla.imageapi.model.domain.ImageTag
import no.ndla.imageapi.ImageApiProperties.TopicAPIUrl
import no.ndla.imageapi.model.Language
import no.ndla.mapping.ISO639.get6391CodeFor6392Code
import org.json4s.native.Serialization.read

import scala.io.Source
import scala.util.Try
import scala.util.matching.Regex

trait TagsService {
  val tagsService: TagsService

  class TagsService {

    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

    def forImage(nid: String): Try[List[ImageTag]] = {
      Try(new URL(TopicAPIUrl + nid).openStream).map(streamToImageTags)
    }

    def streamToImageTags(stream: InputStream) = {
      implicit val formats = org.json4s.DefaultFormats
      read[Keywords](Source.fromInputStream(stream).mkString).keyword
        .flatMap(_.names)
        .flatMap(_.data)
        .flatMap(_.toIterable)
        .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
        .groupBy(_._1)
        .map(entry => (entry._1, entry._2.map(_._2)))
        .map(t => ImageTag(t._2, Language.languageOrUnknown(t._1)))
        .toList
    }

    def getISO639(languageUrl: String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else get6391CodeFor6392Code(x)
        case None    => None
      }
    }
  }

}

case class Keywords(keyword: List[Keyword])

case class Keyword(psi: Option[String],
                   topicId: Option[String],
                   visibility: Option[String],
                   approved: Option[String],
                   processState: Option[String],
                   psis: List[String],
                   originatingSites: List[String],
                   types: List[Any],
                   names: List[KeywordName])

case class Type(typeId: String)

case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String, String]])

case class KeywordNameName(isoLanguageCode: String)
