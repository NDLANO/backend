/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import io.circe.generic.auto.*
import io.circe.syntax.*
import no.ndla.frontpageapi.model.api.{NewSubjectPageDTO, SubjectPageDTO, UpdatedSubjectPageDTO}
import no.ndla.frontpageapi.model.domain.{FilmFrontPage, SubjectPage, VisualElementType}
import no.ndla.frontpageapi.model.{api, domain}

object TestData {

  val domainSubjectPage: SubjectPage = domain.SubjectPage(
    Some(1),
    "Samfunnsfag",
    domain.BannerImage(Some(29668), 29668),
    Seq(
      domain.AboutSubject(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        "nb",
        domain.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(domain.MetaDescription("meta", "nb")),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )
  val domainSubjectJson: String = domainSubjectPage.asJson.noSpaces

  val domainUpdatedSubjectPage: SubjectPage = domain.SubjectPage(
    Some(1),
    "Samfunnsfag",
    domain.BannerImage(Some(29668), 29668),
    Seq(
      domain.AboutSubject(
        "Om Samfunnsfag",
        "Dette er oppdatert om samfunnsfag",
        "nb",
        domain.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(
      domain.MetaDescription("meta", "nb")
    ),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )

  val apiSubjectPage: SubjectPageDTO = api.SubjectPageDTO(
    1,
    "Samfunnsfag",
    api.BannerImageDTO(
      Some("http://api-gateway.ndla-local/image-api/raw/id/29668"),
      Some(29668),
      "http://api-gateway.ndla-local/image-api/raw/id/29668",
      29668
    ),
    Some(
      api.AboutSubjectDTO(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        api.VisualElementDTO("image", "http://api-gateway.ndla-local/image-api/raw/id/123", Some("alt text"))
      )
    ),
    Some("meta"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("nb"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )

  val apiNewSubjectPage: NewSubjectPageDTO = api.NewSubjectPageDTO(
    "Samfunnsfag",
    None,
    api.NewOrUpdateBannerImageDTO(Some(29668), 29668),
    Seq(
      api.NewOrUpdatedAboutSubjectDTO(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        "nb",
        api.NewOrUpdatedVisualElementDTO("image", "123", Some("alt text"))
      )
    ),
    Seq(api.NewOrUpdatedMetaDescriptionDTO("meta", "nb")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"))
  )

  val apiUpdatedSubjectPage: UpdatedSubjectPageDTO = api.UpdatedSubjectPageDTO(
    Some("Samfunnsfag"),
    None,
    Some(api.NewOrUpdateBannerImageDTO(Some(29668), 29668)),
    Some(
      List(
        api.NewOrUpdatedAboutSubjectDTO(
          "Om Samfunnsfag",
          "Dette er oppdatert om samfunnsfag",
          "nb",
          api.NewOrUpdatedVisualElementDTO("image", "123", Some("alt text"))
        )
      )
    ),
    Some(List(api.NewOrUpdatedMetaDescriptionDTO("meta", "nb"))),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"))
  )

  val domainFilmFrontPage: FilmFrontPage = domain.FilmFrontPage(
    "Film",
    Seq(
      domain.AboutSubject(
        "Film",
        "Film faget",
        "nb",
        domain.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      ),
      domain.AboutSubject(
        "Film",
        "Subject film",
        "en",
        domain.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(
      domain.MovieTheme(
        Seq(
          domain.MovieThemeName("Første filmtema", "nb"),
          domain.MovieThemeName("First movie theme", "en")
        ),
        Seq("movieref1", "movieref2")
      )
    ),
    Seq(),
    None
  )

  val apiFilmFrontPage: api.FilmFrontPageDTO = api.FilmFrontPageDTO("", Seq(), Seq(), Seq(), None)
}
