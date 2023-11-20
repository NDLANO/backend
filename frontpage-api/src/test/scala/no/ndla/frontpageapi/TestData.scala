/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import io.circe.generic.auto._
import io.circe.syntax._
import no.ndla.frontpageapi.model.api.{NewSubjectFrontPageData, SubjectPageData, UpdatedSubjectFrontPageData}
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

  val apiSubjectPage: SubjectPageData = api.SubjectPageData(
    1,
    "Samfunnsfag",
    api.BannerImage(
      Some("http://api-gateway.ndla-local/image-api/raw/id/29668"),
      Some(29668),
      "http://api-gateway.ndla-local/image-api/raw/id/29668",
      29668
    ),
    Some(
      api.AboutSubject(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        api.VisualElement("image", "http://api-gateway.ndla-local/image-api/raw/id/123", Some("alt text"))
      )
    ),
    Some("meta"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("nb"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"),
    List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")
  )

  val apiNewSubjectPage: NewSubjectFrontPageData = api.NewSubjectFrontPageData(
    "Samfunnsfag",
    None,
    api.NewOrUpdateBannerImage(Some(29668), 29668),
    Seq(
      api.NewOrUpdatedAboutSubject(
        "Om Samfunnsfag",
        "Dette er samfunnsfag",
        "nb",
        api.NewOrUpdatedVisualElement("image", "123", Some("alt text"))
      )
    ),
    Seq(api.NewOrUpdatedMetaDescription("meta", "nb")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204")),
    Some(List("urn:resource:1:161411", "urn:resource:1:182176", "urn:resource:1:183636", "urn:resource:1:170204"))
  )

  val apiUpdatedSubjectPage: UpdatedSubjectFrontPageData = api.UpdatedSubjectFrontPageData(
    Some("Samfunnsfag"),
    None,
    Some(api.NewOrUpdateBannerImage(Some(29668), 29668)),
    Some(
      List(
        api.NewOrUpdatedAboutSubject(
          "Om Samfunnsfag",
          "Dette er oppdatert om samfunnsfag",
          "nb",
          api.NewOrUpdatedVisualElement("image", "123", Some("alt text"))
        )
      )
    ),
    Some(List(api.NewOrUpdatedMetaDescription("meta", "nb"))),
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

  val apiFilmFrontPage: api.FilmFrontPageData = api.FilmFrontPageData("", Seq(), Seq(), Seq(), None)
}
