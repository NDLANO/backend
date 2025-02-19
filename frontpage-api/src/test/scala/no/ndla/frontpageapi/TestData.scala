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
import no.ndla.common.model
import no.ndla.common.model.api.frontpage.{AboutSubjectDTO, BannerImageDTO, SubjectPageDTO, VisualElementDTO}
import no.ndla.common.model.domain.frontpage
import no.ndla.common.model.domain.frontpage.{AboutSubject, BannerImage, MetaDescription, MovieTheme, MovieThemeName, SubjectPage, VisualElement, VisualElementType}
import no.ndla.frontpageapi.model.api.{NewSubjectPageDTO, UpdatedSubjectPageDTO}
import no.ndla.frontpageapi.model.domain.FilmFrontPage
import no.ndla.frontpageapi.model.{api, domain}

object TestData {

  val domainSubjectPage: SubjectPage = frontpage.SubjectPage(
    Some(1),
    "Samfunnsfag",
    BannerImage(Some(29668), 29668),
    Seq(
      AboutSubject(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        "nb",
        VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(MetaDescription("meta", "nb")),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )
  val domainSubjectJson: String = domainSubjectPage.asJson.noSpaces

  val domainUpdatedSubjectPage: SubjectPage = frontpage.SubjectPage(
    Some(1),
    "Samfunnsfag",
    frontpage.BannerImage(Some(29668), 29668),
    Seq(
      frontpage.AboutSubject(
        "Om Samfunnsfag",
        "Dette er oppdatert om samfunnsfag",
        "nb",
        frontpage.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(
      frontpage.MetaDescription("meta", "nb")
    ),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )

  val apiSubjectPage: SubjectPageDTO = model.api.frontpage.SubjectPageDTO(
    1,
    "Samfunnsfag",
    BannerImageDTO(
      Some("http://api-gateway.ndla-local/image-api/raw/id/29668"),
      Some(29668),
      "http://api-gateway.ndla-local/image-api/raw/id/29668",
      29668
    ),
    Some(
      AboutSubjectDTO(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        VisualElementDTO("image", "http://api-gateway.ndla-local/image-api/raw/id/123", Some("alt text"))
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
      frontpage.AboutSubject(
        "Film",
        "Film faget",
        "nb",
        frontpage.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      ),
      frontpage.AboutSubject(
        "Film",
        "Subject film",
        "en",
        frontpage.VisualElement(VisualElementType.Image, "123", Some("alt text"))
      )
    ),
    Seq(
      MovieTheme(
        Seq(
          MovieThemeName("Første filmtema", "nb"),
          frontpage.MovieThemeName("First movie theme", "en")
        ),
        Seq("movieref1", "movieref2")
      )
    ),
    Seq(),
    None
  )

  val apiFilmFrontPage: api.FilmFrontPageDTO = api.FilmFrontPageDTO("", Seq(), Seq(), Seq(), None)
}
