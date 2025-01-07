/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import cats.implicits.*
import com.scalatsi.TSType.fromCaseClass
import com.scalatsi.TypescriptType.{TSLiteralString, TSString, TSUnion}
import com.scalatsi.{TSIType, TSNamedType, TSType}
import com.scalatsi.dsl.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*

import scala.reflect.runtime.universe.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.api.{DescriptionDTO, TitleDTO}
import no.ndla.searchapi.model.grep.{
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

import scala.reflect.ClassTag
import scala.util.{Success, Try}

@description("Information about a single grep search result entry")
sealed trait GrepResultDTO {
  @description("The grep code") val code: String
  @description("The greps title") val title: TitleDTO
}

object GrepResultDTO {
  implicit val encoder: Encoder[GrepResultDTO] = Encoder.instance[GrepResultDTO] { result =>
    val json = result match {
      case x: GrepKjerneelementDTO      => x.asJson
      case x: GrepKompetansemaalDTO     => x.asJson
      case x: GrepKompetansemaalSettDTO => x.asJson
      case x: GrepLaererplanDTO         => x.asJson
      case x: GrepTverrfagligTemaDTO    => x.asJson
    }
    // NOTE: Adding the discriminator field that scala-tsi generates in the typescript type.
    //       Useful for guarding the type of the object in the frontend.
    json.mapObject(_.add("typename", Json.fromString(result.getClass.getSimpleName)))
  }

  implicit val s1: Schema["GrepLaererplanDTO"]      = Schema.string
  implicit val s2: Schema["GrepTverrfagligTemaDTO"] = Schema.string

  implicit val decoder: Decoder[GrepResultDTO] = List[Decoder[GrepResultDTO]](
    Decoder[GrepKjerneelementDTO].widen,
    Decoder[GrepKompetansemaalDTO].widen,
    Decoder[GrepKompetansemaalSettDTO].widen,
    Decoder[GrepLaererplanDTO].widen,
    Decoder[GrepTverrfagligTemaDTO].widen
  ).reduceLeft(_ or _)

  implicit val s: Schema[GrepResultDTO] = Schema.oneOfWrapped[GrepResultDTO]
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
            kompetansemaalSett = GrepReferencedKompetansemaalSettDTO(
              code = goal.`tilhoerer-kompetansemaalsett`.kode,
              title = goal.`tilhoerer-kompetansemaalsett`.tittel
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
            title = title,
            kompetansemaal = goalSet.kompetansemaal.map { goal =>
              GrepReferencedKompetansemaalDTO(
                code = goal.kode,
                title = goal.tittel
              )
            }
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
case class GrepReferencedKompetansemaalDTO(code: String, title: String)
case class GrepKjerneelementDTO(
    code: String,
    title: TitleDTO,
    description: DescriptionDTO,
    laereplan: GrepLaererplanDTO
) extends GrepResultDTO
case class GrepKompetansemaalDTO(
    code: String,
    title: TitleDTO,
    laereplan: GrepLaererplanDTO,
    kompetansemaalSett: GrepReferencedKompetansemaalSettDTO,
    tverrfagligeTemaer: List[GrepTverrfagligTemaDTO],
    kjerneelementer: List[GrepReferencedKjerneelementDTO]
) extends GrepResultDTO
case class GrepReferencedKompetansemaalSettDTO(
    code: String,
    title: String
)
case class GrepKompetansemaalSettDTO(
    code: String,
    title: TitleDTO,
    kompetansemaal: List[GrepReferencedKompetansemaalDTO]
) extends GrepResultDTO
case class GrepLaererplanDTO(
    code: String,
    title: TitleDTO,
    typename: "GrepLaererplanDTO" = "GrepLaererplanDTO"
) extends GrepResultDTO
case class GrepTverrfagligTemaDTO(
    code: String,
    title: TitleDTO,
    typename: "GrepTverrfagligTemaDTO" = "GrepTverrfagligTemaDTO"
) extends GrepResultDTO
