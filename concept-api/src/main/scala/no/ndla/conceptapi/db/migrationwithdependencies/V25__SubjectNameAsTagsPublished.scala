/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Json, parser}
import no.ndla.common.CirceUtil
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.database.DocumentMigration
import sttp.client3.quick.*
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps

class V25__SubjectNameAsTagsPublished(
    properties: ConceptApiProperties,
    prefetchedSubjects: Option[List[TaxonomySubject]] = None
) extends DocumentMigration
    with StrictLogging {
  override val columnName: String = "document"
  override val tableName: String  = "publishedconceptdata"

  def toMap(subject: TaxonomySubject): Map[String, String] =
    subject.translations
      .map(t => t.language -> t.name)
      .toMap
      .withDefaultValue(subject.name)

  lazy val subjects: List[TaxonomySubject] = prefetchedSubjects match {
    case Some(value) => value
    case None        =>
      val request  = quickRequest.get(uri"${properties.TaxonomyUrl}/v1/nodes?nodeType=SUBJECT")
      val response = simpleHttpClient.send(request)
      CirceUtil.unsafeParseAs[List[TaxonomySubject]](response.body)
  }

  lazy val subjectIdToTranslationsMap: Map[String, Map[String, String]] = {
    subjects.map { subject => subject.id -> toMap(subject) }.toMap
  }

  private def getLanguagesOfField(fieldName: String, json: Json): List[String] = {
    json.hcursor.downField(fieldName).as[Option[List[LanguageObject]]].toTry.get match {
      case Some(languageObjects) if languageObjects.nonEmpty => languageObjects.map(_.language)
      case _                                                 => List.empty
    }
  }

  def getLanguages(json: Json): List[String] = {
    val fields = List("title", "content", "tags", "visualElement", "metaImage")
    fields.flatMap(field => getLanguagesOfField(field, json)).distinct
  }

  def getTags(json: Json): List[TagsObject] = {
    json.hcursor.downField("tags").as[Option[List[TagsObject]]].toTry.get.getOrElse(List.empty)
  }

  override def convertColumn(document: String): String = {
    val oldDocument  = parser.parse(document).toTry.get
    val languages    = getLanguages(oldDocument)
    val existingTags = getTags(oldDocument)
    oldDocument.hcursor.downField("subjectIds").as[Option[List[String]]].toTry.get match {
      case Some(subjectIds) if subjectIds.nonEmpty =>
        val newTags = subjectIds.foldLeft(existingTags) { case (accTags, sid) =>
          if (subjectIdToTranslationsMap.contains(sid)) {
            val sidTranslations = subjectIdToTranslationsMap(sid)
            languages.map { lang =>
              val tr = sidTranslations(lang)
              val t  = accTags.find(_.language == lang).getOrElse(TagsObject(List.empty, lang))
              t.copy(tags = t.tags :+ tr)
            }
          } else {
            logger.error(s"Subject with id '$sid' not found when running '${getClass.getSimpleName}'")
            accTags
          }
        }

        oldDocument.mapObject {
          case o if !o.contains("tags") => o
          case o                        => o.remove("tags").add("tags", newTags.asJson)
        }.noSpaces

      case _ => document
    }
  }
}
