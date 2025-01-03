/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.search.model.{LanguageValue, SearchableLanguageValues}
import no.ndla.searchapi.model.api.{DescriptionDTO, TitleDTO}
import no.ndla.searchapi.model.grep.{
  GrepElement,
  GrepKjerneelement,
  GrepKompetansemaal,
  GrepKompetansemaalSett,
  GrepLaererplan,
  GrepTitle,
  GrepTverrfagligTema
}
import no.ndla.searchapi.model.search.SearchableGrepElement
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

import scala.util.{Success, Try}

@description("Information about a single grep search result entry")
sealed trait GrepResultDTO {
  @description("The grep code") val code: String
  @description("The greps title") val title: TitleDTO
  val grepType: String
}

object GrepResultDTO {
  implicit val encoder: Encoder[GrepResultDTO] = Encoder.instance {
    case x: GrepKjerneelementDTO      => x.asJson
    case x: GrepKompetansemaalDTO     => x.asJson
    case x: GrepKompetansemaalSettDTO => x.asJson
    case x: GrepLaererplanDTO         => x.asJson
    case x: GrepTverrfagligTemaDTO    => x.asJson
  }
  implicit val decoder: Decoder[GrepResultDTO] = List[Decoder[GrepResultDTO]](
    Decoder[GrepKjerneelementDTO].widen,
    Decoder[GrepKompetansemaalDTO].widen,
    Decoder[GrepKompetansemaalSettDTO].widen,
    Decoder[GrepLaererplanDTO].widen,
    Decoder[GrepTverrfagligTemaDTO].widen
  ).reduceLeft(_ or _)

  // TODO:
  implicit val schema: Schema[GrepResultDTO] = Schema.string

  def fromSearchable(searchable: SearchableGrepElement, language: String): Try[GrepResultDTO] = {
    val titleLv = findByLanguageOrBestEffort(searchable.title.languageValues, language)
      .getOrElse(LanguageValue(Language.DefaultLanguage, ""))
    val title = TitleDTO(title = titleLv.value, language = titleLv.language)

    searchable.domainObject match {
      case core: GrepKjerneelement =>
        val descriptionLvs = GrepTitle.convertTitles(core.beskrivelse.tekst.toSeq)
        val descriptionLv: LanguageValue[String] =
          findByLanguageOrBestEffort(descriptionLvs, language)
            .getOrElse(LanguageValue(Language.DefaultLanguage, ""))
        val description = DescriptionDTO(description = descriptionLv.value, language = descriptionLv.language)

        Success(
          GrepKjerneelementDTO(
            code = core.kode,
            title = title,
            description = description,
            laereplan = GrepLaererplanDTO(
              code = core.`tilhoerer-laereplan`.kode,
              title = TitleDTO(core.`tilhoerer-laereplan`.tittel, Language.DefaultLanguage)
            )
          )
        )
      case goal: GrepKompetansemaal =>
        Success(
          GrepKompetansemaalDTO(
            code = goal.kode,
            title = title,
            laereplan = GrepLaererplanDTO(
              code = goal.`tilhoerer-laereplan`.kode,
              title = TitleDTO(goal.`tilhoerer-laereplan`.tittel, Language.DefaultLanguage)
            ),
            kompetansemaalSett = GrepKompetansemaalSettDTO(
              code = goal.`tilhoerer-kompetansemaalsett`.kode,
              title = TitleDTO(goal.`tilhoerer-kompetansemaalsett`.tittel, Language.DefaultLanguage)
            ),
            tverrfagligeTemaer = goal.`tilknyttede-tverrfaglige-temaer`.map { crossTopic =>
              GrepTverrfagligTemaDTO(
                code = crossTopic.referanse.kode,
                title = TitleDTO(crossTopic.referanse.tittel, Language.DefaultLanguage)
              )
            },
            kjerneelementer = goal.`tilknyttede-kjerneelementer`.map { core =>
              GrepReferencedKjerneelementDTO(
                code = core.referanse.kode,
                title = core.referanse.tittel
              )
            }
          )
        )
      case goalSet: GrepKompetansemaalSett =>
        Success(
          GrepKompetansemaalSettDTO(
            code = goalSet.kode,
            title = title
          )
        )
      case curriculum: GrepLaererplan =>
        Success(
          GrepLaererplanDTO(
            code = curriculum.kode,
            title = title
          )
        )
      case crossTopic: GrepTverrfagligTema =>
        Success(
          GrepTverrfagligTemaDTO(
            code = crossTopic.kode,
            title = title
          )
        )
    }
  }
}

case class GrepReferencedKjerneelementDTO(code: String, title: String)
case class GrepKjerneelementDTO(
    code: String,
    title: TitleDTO,
    description: DescriptionDTO,
    laereplan: GrepLaererplanDTO,
    grepType: "kjerneelement" = "kjerneelement"
) extends GrepResultDTO
case class GrepKompetansemaalDTO(
    code: String,
    title: TitleDTO,
    laereplan: GrepLaererplanDTO,
    kompetansemaalSett: GrepKompetansemaalSettDTO,
    tverrfagligeTemaer: List[GrepTverrfagligTemaDTO],
    kjerneelementer: List[GrepReferencedKjerneelementDTO],
    grepType: "kompetansemaal" = "kompetansemaal"
) extends GrepResultDTO
case class GrepKompetansemaalSettDTO(
    code: String,
    title: TitleDTO,
    grepType: "kompetansemaalsett" = "kompetansemaalsett"
) extends GrepResultDTO
case class GrepLaererplanDTO(
    code: String,
    title: TitleDTO,
    grepType: "laererplan" = "laererplan"
) extends GrepResultDTO
case class GrepTverrfagligTemaDTO(
    code: String,
    title: TitleDTO,
    grepType: "tverrfaglig-tema" = "tverrfaglig-tema"
) extends GrepResultDTO
