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
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.TapirUtil.stringLiteralSchema
import no.ndla.common.model.api.search.{LanguageValue, TitleDTO}
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.searchapi.model.api.DescriptionDTO
import no.ndla.searchapi.model.grep.{
  GrepFagkode,
  GrepKjerneelement,
  GrepKompetansemaal,
  GrepKompetansemaalSett,
  GrepLaererplan,
  GrepTitle,
  GrepTverrfagligTema,
}
import no.ndla.searchapi.model.search.SearchableGrepElement
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

import scala.util.{Success, Try}

@description("Information about a single grep search result entry")
sealed trait GrepResultDTO {
  @description("The grep code")
  val code: String
  @description("The grep uri")
  val uri: String
  @description("The grep status")
  val status: GrepStatusDTO
  @description("The greps title")
  val title: TitleDTO
}

object GrepResultDTO {
  implicit val encoder: Encoder[GrepResultDTO] = Encoder.instance[GrepResultDTO] { result =>
    val json = result match {
      case x: GrepKjerneelementDTO      => x.asJson
      case x: GrepKompetansemaalDTO     => x.asJson
      case x: GrepKompetansemaalSettDTO => x.asJson
      case x: GrepLaererplanDTO         => x.asJson
      case x: GrepTverrfagligTemaDTO    => x.asJson
      case x: GrepFagkodeDTO            => x.asJson
    }
    // NOTE: Adding the discriminator field that scala-tsi generates in the typescript type.
    //       Useful for guarding the type of the object in the frontend.
    CirceUtil.addTypenameDiscriminator(json, result.getClass)
  }

  implicit val s1: Schema["GrepLaererplanDTO"]         = stringLiteralSchema("GrepLaererplanDTO")
  implicit val s2: Schema["GrepTverrfagligTemaDTO"]    = stringLiteralSchema("GrepTverrfagligTemaDTO")
  implicit val s3: Schema["GrepKompetansemaalSettDTO"] = stringLiteralSchema("GrepKompetansemaalSettDTO")
  implicit val s4: Schema["GrepKompetansemaalDTO"]     = stringLiteralSchema("GrepKompetansemaalDTO")
  implicit val s5: Schema["GrepKjerneelementDTO"]      = stringLiteralSchema("GrepKjerneelementDTO")
  implicit val s6: Schema["GrepFagkodeDTO"]            = stringLiteralSchema("GrepFagkodeDTO")

  implicit val decoder: Decoder[GrepResultDTO] = List[Decoder[GrepResultDTO]](
    Decoder[GrepKjerneelementDTO].widen,
    Decoder[GrepKompetansemaalDTO].widen,
    Decoder[GrepKompetansemaalSettDTO].widen,
    Decoder[GrepLaererplanDTO].widen,
    Decoder[GrepTverrfagligTemaDTO].widen,
    Decoder[GrepFagkodeDTO].widen,
  ).reduceLeft(_ or _)

  def fromSearchable(searchable: SearchableGrepElement, language: String): Try[GrepResultDTO] = {
    val titleLv = findByLanguageOrBestEffort(searchable.title.languageValues, language).getOrElse(
      LanguageValue(Language.DefaultLanguage, "")
    )
    val title = TitleDTO(title = titleLv.value, language = titleLv.language)

    searchable.domainObject match {
      case core: GrepKjerneelement =>
        val descriptionLvs                       = GrepTitle.convertTitles(core.beskrivelse.tekst.toSeq)
        val descriptionLv: LanguageValue[String] =
          findByLanguageOrBestEffort(descriptionLvs, language).getOrElse(LanguageValue(Language.DefaultLanguage, ""))
        val description = DescriptionDTO(description = descriptionLv.value, language = descriptionLv.language)

        Success(
          GrepKjerneelementDTO(
            code = core.kode,
            uri = core.uri,
            status = core.status,
            title = title,
            description = description,
            laereplan = GrepReferencedLaereplanDTO(
              code = core.`tilhoerer-laereplan`.kode,
              uri = core.`tilhoerer-laereplan`.uri,
              status = core.`tilhoerer-laereplan`.status,
              title = core.`tilhoerer-laereplan`.tittel,
            ),
          )
        )
      case goal: GrepKompetansemaal => Success(
          GrepKompetansemaalDTO(
            code = goal.kode,
            uri = goal.uri,
            status = goal.status,
            title = title,
            laereplan = GrepReferencedLaereplanDTO(
              code = goal.`tilhoerer-laereplan`.kode,
              uri = goal.`tilhoerer-laereplan`.uri,
              status = goal.`tilhoerer-laereplan`.status,
              title = goal.`tilhoerer-laereplan`.tittel,
            ),
            kompetansemaalSett = GrepReferencedKompetansemaalSettDTO(
              code = goal.`tilhoerer-kompetansemaalsett`.kode,
              uri = goal.`tilhoerer-kompetansemaalsett`.uri,
              status = goal.`tilhoerer-kompetansemaalsett`.status,
              title = goal.`tilhoerer-kompetansemaalsett`.tittel,
            ),
            tverrfagligeTemaer = goal
              .`tilknyttede-tverrfaglige-temaer`
              .map { crossTopic =>
                GrepTverrfagligTemaDTO(
                  code = crossTopic.referanse.kode,
                  uri = crossTopic.referanse.uri,
                  status = crossTopic.referanse.status,
                  title = TitleDTO(crossTopic.referanse.tittel, Language.DefaultLanguage),
                )
              },
            kjerneelementer = goal
              .`tilknyttede-kjerneelementer`
              .map { core =>
                GrepReferencedKjerneelementDTO(
                  code = core.referanse.kode,
                  uri = core.referanse.uri,
                  status = core.referanse.status,
                  title = core.referanse.tittel,
                )
              },
            reuseOf = goal
              .`gjenbruk-av`
              .map { goal =>
                GrepReferencedKompetansemaalDTO(
                  code = goal.kode,
                  uri = goal.uri,
                  status = goal.status,
                  title = goal.tittel,
                )
              },
          )
        )
      case goalSet: GrepKompetansemaalSett => Success(
          GrepKompetansemaalSettDTO(
            code = goalSet.kode,
            uri = goalSet.uri,
            status = goalSet.status,
            title = title,
            kompetansemaal = goalSet
              .kompetansemaal
              .map { goal =>
                GrepReferencedKompetansemaalDTO(
                  code = goal.kode,
                  uri = goal.uri,
                  status = goal.status,
                  title = goal.tittel,
                )
              },
          )
        )
      case curriculum: GrepLaererplan => Success(
          GrepLaererplanDTO(
            code = curriculum.kode,
            uri = curriculum.uri,
            status = curriculum.status,
            title = title,
            replacedBy = curriculum
              .`erstattes-av`
              .map(replacement =>
                GrepReferencedLaereplanDTO(
                  code = replacement.kode,
                  uri = replacement.uri,
                  status = replacement.status,
                  title = replacement.tittel,
                )
              ),
          )
        )
      case fagKode: GrepFagkode =>
        val kortformLvs                       = GrepTitle.convertTitles(fagKode.kortform)
        val kortformLv: LanguageValue[String] =
          findByLanguageOrBestEffort(kortformLvs, language).getOrElse(LanguageValue(Language.DefaultLanguage, ""))
        val kortform = TitleDTO(title = kortformLv.value, language = kortformLv.language)

        Success(
          GrepFagkodeDTO(
            code = fagKode.kode,
            uri = fagKode.uri,
            status = fagKode.status,
            title = title,
            kortform = kortform,
          )
        )
      case crossTopic: GrepTverrfagligTema => Success(
          GrepTverrfagligTemaDTO(
            code = crossTopic.kode,
            uri = crossTopic.uri,
            status = crossTopic.status,
            title = title,
          )
        )

    }
  }
}

case class GrepReferencedKjerneelementDTO(code: String, uri: String, status: GrepStatusDTO, title: String)
case class GrepReferencedKompetansemaalDTO(code: String, uri: String, status: GrepStatusDTO, title: String)
case class GrepReferencedLaereplanDTO(code: String, uri: String, status: GrepStatusDTO, title: String)
case class GrepKjerneelementDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    description: DescriptionDTO,
    laereplan: GrepReferencedLaereplanDTO,
    typename: "GrepKjerneelementDTO" = "GrepKjerneelementDTO",
) extends GrepResultDTO
case class GrepKompetansemaalDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    laereplan: GrepReferencedLaereplanDTO,
    kompetansemaalSett: GrepReferencedKompetansemaalSettDTO,
    tverrfagligeTemaer: List[GrepTverrfagligTemaDTO],
    kjerneelementer: List[GrepReferencedKjerneelementDTO],
    reuseOf: Option[GrepReferencedKompetansemaalDTO],
    typename: "GrepKompetansemaalDTO" = "GrepKompetansemaalDTO",
) extends GrepResultDTO
case class GrepReferencedKompetansemaalSettDTO(code: String, uri: String, status: GrepStatusDTO, title: String)
case class GrepKompetansemaalSettDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    kompetansemaal: List[GrepReferencedKompetansemaalDTO],
    typename: "GrepKompetansemaalSettDTO" = "GrepKompetansemaalSettDTO",
) extends GrepResultDTO
case class GrepLaererplanDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    replacedBy: List[GrepReferencedLaereplanDTO],
    typename: "GrepLaererplanDTO" = "GrepLaererplanDTO",
) extends GrepResultDTO
case class GrepTverrfagligTemaDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    typename: "GrepTverrfagligTemaDTO" = "GrepTverrfagligTemaDTO",
) extends GrepResultDTO
case class GrepFagkodeDTO(
    code: String,
    uri: String,
    status: GrepStatusDTO,
    title: TitleDTO,
    kortform: TitleDTO,
    typename: "GrepFagkodeDTO" = "GrepFagkodeDTO",
) extends GrepResultDTO
