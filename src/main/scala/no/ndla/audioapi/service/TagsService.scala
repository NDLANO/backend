/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.Tag
import no.ndla.audioapi.AudioApiProperties.TopicAPIUrl
import no.ndla.audioapi.model.Language
import no.ndla.mapping.ISO639

import scala.io.Source
import scala.util.matching.Regex

trait TagsService {
  val tagsService: TagsService

  val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

  class TagsService {
    def forAudio(nid: String): List[Tag] = {
      val jsonString = Source.fromURL(TopicAPIUrl + nid).mkString
      keywordsJsonToImageTags(jsonString)
    }

    def keywordsJsonToImageTags(keywordsJson: String): List[Tag] = {
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      read[Keywords](keywordsJson)
        .keyword
        .flatMap(_.names)
        .flatMap(_.data)
        .flatMap(_.toIterable)
        .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
        .groupBy(_._1).map(entry => (entry._1, entry._2.map(_._2)))
        .map(t => Tag(t._2, Language.languageOrUnknown(t._1))).toList
    }

    def getISO639(languageUrl:String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else ISO639.get6391CodeFor6392Code(x)
        case None => None
      }
    }
  }

}

case class Keywords(keyword: List[Keyword])
case class Keyword(psi: Option[String], topicId: Option[String], visibility: Option[String], approved: Option[String], processState: Option[String], psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])

case class Type(typeId:String)
case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String,String]])
case class KeywordNameName(isoLanguageCode: String)
