/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import io.circe.generic.auto._
import io.circe.syntax._
import no.ndla.frontpageapi.model.api.{NewSubjectFrontPageDataDTO, SubjectPageDataDTO, UpdatedSubjectFrontPageDataDTO}
import no.ndla.frontpageapi.model.domain.{FilmFrontPageData, SubjectFrontPageData, VisualElementType}
import no.ndla.frontpageapi.model.{api, domain}

object TestData {

  val domainSubjectPage: SubjectFrontPageData = domain.SubjectFrontPageData(
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

  val domainUpdatedSubjectPage: SubjectFrontPageData = domain.SubjectFrontPageData(
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

  val apiSubjectPage: SubjectPageDataDTO = api.SubjectPageDataDTO(
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

  val apiNewSubjectPage: NewSubjectFrontPageDataDTO = api.NewSubjectFrontPageDataDTO(
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

  val apiUpdatedSubjectPage: UpdatedSubjectFrontPageDataDTO = api.UpdatedSubjectFrontPageDataDTO(
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

  val domainFilmFrontPage: FilmFrontPageData = domain.FilmFrontPageData(
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

  val apiFilmFrontPage: api.FilmFrontPageDataDTO = api.FilmFrontPageDataDTO("", Seq(), Seq(), Seq(), None)
}
