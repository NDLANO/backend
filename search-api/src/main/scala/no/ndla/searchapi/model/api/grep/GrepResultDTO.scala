/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import cats.implicits.*
import com.scalatsi.TypescriptType.TSUnion
import com.scalatsi.{TSNamedType, TSType, TypescriptType}
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.TitleDTO
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.api.DescriptionDTO
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
    CirceUtil.addTypenameDiscriminator(json, result.getClass)
  }

  val typescriptUnionTypes: Seq[TypescriptType.TSInterface] = Seq(
    TSType.fromCaseClass[GrepKjerneelementDTO].get,
    TSType.fromCaseClass[GrepKompetansemaalDTO].get,
    TSType.fromCaseClass[GrepKompetansemaalSettDTO].get,
    TSType.fromCaseClass[GrepLaererplanDTO].get,
    TSType.fromCaseClass[GrepTverrfagligTemaDTO].get
  )

  implicit val t2: TSNamedType[GrepResultDTO] =
    TSType.alias[GrepResultDTO]("GrepResultDTO", TSUnion(typescriptUnionTypes))

  implicit val s1: Schema["GrepLaererplanDTO"]         = Schema.string
  implicit val s2: Schema["GrepTverrfagligTemaDTO"]    = Schema.string
  implicit val s3: Schema["GrepKompetansemaalSettDTO"] = Schema.string
  implicit val s4: Schema["GrepKompetansemaalDTO"]     = Schema.string
  implicit val s5: Schema["GrepKjerneelementDTO"]      = Schema.string

  implicit val decoder: Decoder[GrepResultDTO] = List[Decoder[GrepResultDTO]](
    Decoder[GrepKjerneelementDTO].widen,
    Decoder[GrepKompetansemaalDTO].widen,
    Decoder[GrepKompetansemaalSettDTO].widen,
    Decoder[GrepLaererplanDTO].widen,
    Decoder[GrepTverrfagligTemaDTO].widen
  ).reduceLeft(_ or _)

  def fromSearchable(searchable: SearchableGrepElement, language: String): Try[GrepResultDTO] = {
    val titleLv = findByLanguageOrBestEffort(searchable.title.languageValues, language)
      .getOrElse(LanguageValue(Language.DefaultLanguage, ""))
    val title = TitleDTO(title = titleLv.value, language = titleLv.language)

    searchable.domainObject match {
      case core: GrepKjerneelement =>
        val descriptionLvs = GrepTitle.convertTitles(core.beskrivelse.tekst.toSeq)
        val descriptionLv: LanguageValue[String] =
          findByLanguageOrBestEffort(descriptionLvs, language).getOrElse(LanguageValue(Language.DefaultLanguage, ""))
        val description = DescriptionDTO(description = descriptionLv.value, language = descriptionLv.language)

        Success(
          GrepKjerneelementDTO(
            code = core.kode,
            title = title,
            description = description,
            laereplan = GrepReferencedLaereplanDTO(
              code = core.`tilhoerer-laereplan`.kode,
              title = core.`tilhoerer-laereplan`.tittel
            )
          )
        )
      case goal: GrepKompetansemaal =>
        Success(
          GrepKompetansemaalDTO(
            code = goal.kode,
            title = title,
            laereplan = GrepReferencedLaereplanDTO(
              code = goal.`tilhoerer-laereplan`.kode,
              title = goal.`tilhoerer-laereplan`.tittel
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
            },
            reuseOf = goal.`gjenbruk-av`.map { goal =>
              GrepReferencedKompetansemaalDTO(
                code = goal.kode,
                title = goal.tittel
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
            title = title,
            replacedBy = curriculum.`erstattes-av`.map(replacement =>
              GrepReferencedLaereplanDTO(
                code = replacement.kode,
                title = replacement.tittel
              )
            )
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
case class GrepReferencedLaereplanDTO(code: String, title: String)
case class GrepKjerneelementDTO(
    code: String,
    title: TitleDTO,
    description: DescriptionDTO,
    laereplan: GrepReferencedLaereplanDTO,
    typename: "GrepKjerneelementDTO" = "GrepKjerneelementDTO"
) extends GrepResultDTO
case class GrepKompetansemaalDTO(
    code: String,
    title: TitleDTO,
    laereplan: GrepReferencedLaereplanDTO,
    kompetansemaalSett: GrepReferencedKompetansemaalSettDTO,
    tverrfagligeTemaer: List[GrepTverrfagligTemaDTO],
    kjerneelementer: List[GrepReferencedKjerneelementDTO],
    reuseOf: Option[GrepReferencedKompetansemaalDTO],
    typename: "GrepKompetansemaalDTO" = "GrepKompetansemaalDTO"
) extends GrepResultDTO
case class GrepReferencedKompetansemaalSettDTO(
    code: String,
    title: String
)
case class GrepKompetansemaalSettDTO(
    code: String,
    title: TitleDTO,
    kompetansemaal: List[GrepReferencedKompetansemaalDTO],
    typename: "GrepKompetansemaalSettDTO" = "GrepKompetansemaalSettDTO"
) extends GrepResultDTO
case class GrepLaererplanDTO(
    code: String,
    title: TitleDTO,
    replacedBy: List[GrepReferencedLaereplanDTO],
    typename: "GrepLaererplanDTO" = "GrepLaererplanDTO"
) extends GrepResultDTO
case class GrepTverrfagligTemaDTO(
    code: String,
    title: TitleDTO,
    typename: "GrepTverrfagligTemaDTO" = "GrepTverrfagligTemaDTO"
) extends GrepResultDTO
