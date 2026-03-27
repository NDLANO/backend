/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import cats.implicits.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.{Decoder, Encoder}
import no.ndla.language.Language.getSupportedLanguages

import scala.util.Try

case class SubjectPage(
    id: Option[Long],
    name: String,
    bannerImage: BannerImage,
    about: Seq[AboutSubject],
    metaDescription: Seq[MetaDescription],
    editorsChoices: List[String],
    connectedTo: List[String],
    buildsOn: List[String],
    leadsTo: List[String],
    popularArticles: Seq[PopularArticle] = Seq.empty,
) {

  def supportedLanguages: Seq[String] = getSupportedLanguages(about, metaDescription)

}

object SubjectPage {
  implicit val encoder: Encoder[SubjectPage] = deriveEncoder
  implicit val decoder: Decoder[SubjectPage] = deriveDecoder[SubjectPage].prepare(
    _.withFocus(
      _.mapObject(obj =>
        if (obj.contains("popularArticles")) obj
        else obj.add("popularArticles", io.circe.Json.arr())
      )
    )
  )
  def decodeJson(json: String, id: Long): Try[SubjectPage] = {
    parse(json).flatMap(_.as[SubjectPage]).map(_.copy(id = id.some)).toTry
  }
}
