/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.model.domain.Errors.LanguageNotFoundException
import no.ndla.frontpageapi.model.{api, domain}

import scala.util.{Failure, Success, Try}
import cats.implicits.*
import no.ndla.common.errors.MissingIdException
import no.ndla.common.model
import no.ndla.common.model.api.frontpage.{AboutSubjectDTO, BannerImageDTO, SubjectPageDTO, VisualElementDTO}
import no.ndla.common.model.domain.frontpage
import no.ndla.common.model.domain.frontpage.{
  AboutSubject,
  BannerImage,
  MetaDescription,
  MovieTheme,
  MovieThemeName,
  SubjectPage,
  VisualElement,
  VisualElementType
}
import no.ndla.frontpageapi.Props
import no.ndla.language.Language.{findByLanguageOrBestEffort, mergeLanguageFields}

class ConverterService(using props: Props) {
  private def toApiMenu(menu: domain.Menu): model.api.MenuDTO =
    model.api.MenuDTO(menu.articleId, menu.menu.map(toApiMenu), Some(menu.hideLevel))

  def toApiFrontPage(frontPage: domain.FrontPage): model.api.FrontPageDTO =
    model.api.FrontPageDTO(articleId = frontPage.articleId, menu = frontPage.menu.map(toApiMenu))

  private def toApiBannerImage(banner: BannerImage): BannerImageDTO =
    model.api.frontpage.BannerImageDTO(
      banner.mobileImageId.map(createImageUrl),
      banner.mobileImageId,
      createImageUrl(banner.desktopImageId),
      banner.desktopImageId
    )

  def toApiSubjectPage(
      sub: SubjectPage,
      language: String,
      fallback: Boolean = false
  ): Try[SubjectPageDTO] = {
    if (sub.supportedLanguages.contains(language) || fallback) {
      sub.id match {
        case None => Failure(MissingIdException("No id found for subjectpage while converting, this is a bug."))
        case Some(subjectPageId) =>
          Success(
            SubjectPageDTO(
              subjectPageId,
              sub.name,
              toApiBannerImage(sub.bannerImage),
              toApiAboutSubject(findByLanguageOrBestEffort(sub.about, language)),
              toApiMetaDescription(findByLanguageOrBestEffort(sub.metaDescription, language)),
              sub.editorsChoices,
              sub.supportedLanguages,
              sub.connectedTo,
              sub.buildsOn,
              sub.leadsTo
            )
          )
      }
    } else {
      Failure(
        LanguageNotFoundException(
          s"The subjectpage with id ${sub.id.get} and language $language was not found",
          sub.supportedLanguages
        )
      )
    }
  }

  def toApiFilmFrontPage(page: domain.FilmFrontPage, language: Option[String]): api.FilmFrontPageDTO = {
    api.FilmFrontPageDTO(
      page.name,
      toApiAboutFilmSubject(page.about, language),
      toApiMovieThemes(page.movieThemes, language),
      page.slideShow,
      page.article,
      page.supportedLanguages
    )
  }

  private def toApiAboutFilmSubject(
      aboutSeq: Seq[AboutSubject],
      language: Option[String]
  ): Seq[api.AboutFilmSubjectDTO] = {
    val filteredAboutSeq = language match {
      case Some(lang) => aboutSeq.filter(about => about.language == lang)
      case None       => aboutSeq
    }
    filteredAboutSeq.map(about =>
      api.AboutFilmSubjectDTO(about.title, about.description, toApiVisualElement(about.visualElement), about.language)
    )
  }

  private def toApiMovieThemes(themes: Seq[MovieTheme], language: Option[String]): Seq[api.MovieThemeDTO] = {
    themes.map(theme => api.MovieThemeDTO(toApiMovieName(theme.name, language), theme.movies))
  }

  private def toApiMovieName(
      names: Seq[MovieThemeName],
      language: Option[String]
  ): Seq[api.MovieThemeNameDTO] = {
    val filteredNames = language match {
      case Some(lang) => names.filter(name => name.language == lang)
      case None       => names
    }

    filteredNames.map(name => api.MovieThemeNameDTO(name.name, name.language))
  }

  private def toApiAboutSubject(about: Option[AboutSubject]): Option[AboutSubjectDTO] = {
    about
      .map(about => AboutSubjectDTO(about.title, about.description, toApiVisualElement(about.visualElement)))
  }

  private def toApiMetaDescription(meta: Option[MetaDescription]): Option[String] = {
    meta
      .map(_.metaDescription)
  }

  private def toApiVisualElement(visual: VisualElement): VisualElementDTO = {
    val url = visual.`type` match {
      case VisualElementType.Image      => createImageUrl(visual.id.toLong)
      case VisualElementType.Brightcove =>
        s"https://players.brightcove.net/${props.BrightcoveAccountId}/${props.BrightcovePlayer}_default/index.html?videoId=${visual.id}"
    }
    VisualElementDTO(visual.`type`.entryName, url, visual.alt)
  }

  def toDomainSubjectPage(id: Long, subject: api.NewSubjectPageDTO): Try[SubjectPage] =
    toDomainSubjectPage(subject).map(_.copy(id = Some(id)))

  private def toDomainBannerImage(banner: api.NewOrUpdateBannerImageDTO): BannerImage =
    frontpage.BannerImage(banner.mobileImageId, banner.desktopImageId)

  def toDomainSubjectPage(subject: api.NewSubjectPageDTO): Try[SubjectPage] = {
    for {
      about <- toDomainAboutSubject(subject.about)
      newSubject = frontpage.SubjectPage(
        id = None,
        name = subject.name,
        bannerImage = toDomainBannerImage(subject.banner),
        about = about,
        metaDescription = toDomainMetaDescription(subject.metaDescription),
        editorsChoices = subject.editorsChoices.getOrElse(List()),
        connectedTo = subject.connectedTo.getOrElse(List()),
        buildsOn = subject.buildsOn.getOrElse(List()),
        leadsTo = subject.leadsTo.getOrElse(List())
      )

    } yield newSubject
  }

  def toDomainSubjectPage(
      toMergeInto: SubjectPage,
      subject: api.UpdatedSubjectPageDTO
  ): Try[SubjectPage] = {
    for {
      aboutSubject <- subject.about.traverse(toDomainAboutSubject)
      metaDescription = subject.metaDescription.map(toDomainMetaDescription)

      merged = toMergeInto.copy(
        name = subject.name.getOrElse(toMergeInto.name),
        bannerImage = subject.banner.map(toDomainBannerImage).getOrElse(toMergeInto.bannerImage),
        about = mergeLanguageFields(toMergeInto.about, aboutSubject.toSeq.flatten),
        metaDescription = mergeLanguageFields(toMergeInto.metaDescription, metaDescription.toSeq.flatten),
        editorsChoices = subject.editorsChoices.getOrElse(toMergeInto.editorsChoices),
        connectedTo = subject.connectedTo.getOrElse(toMergeInto.connectedTo),
        buildsOn = subject.buildsOn.getOrElse(toMergeInto.buildsOn),
        leadsTo = subject.leadsTo.getOrElse(toMergeInto.leadsTo)
      )
    } yield merged
  }

  private def toDomainAboutSubject(aboutSeq: Seq[api.NewOrUpdatedAboutSubjectDTO]): Try[Seq[AboutSubject]] = {
    aboutSeq.traverse(about =>
      toDomainVisualElement(about.visualElement)
        .map(frontpage.AboutSubject(about.title, about.description, about.language, _))
    )
  }

  private def toDomainMetaDescription(
      metaSeq: Seq[api.NewOrUpdatedMetaDescriptionDTO]
  ): Seq[MetaDescription] = {
    metaSeq.map(meta => frontpage.MetaDescription(meta.metaDescription, meta.language))
  }

  private def toDomainVisualElement(visual: api.NewOrUpdatedVisualElementDTO): Try[VisualElement] =
    for {
      t <- VisualElementType.fromString(visual.`type`)
      ve = frontpage.VisualElement(t, visual.id, visual.alt)
      validated <- VisualElementType.validateVisualElement(ve)
    } yield validated

  private def toDomainMenu(menu: model.api.MenuDTO): domain.Menu = {
    val apiMenu = menu.menu.map { case x: model.api.MenuDTO => toDomainMenu(x) }
    domain.Menu(articleId = menu.articleId, menu = apiMenu, hideLevel = menu.hideLevel.getOrElse(false))
  }

  def toDomainFrontPage(page: model.api.FrontPageDTO): domain.FrontPage = {
    domain.FrontPage(articleId = page.articleId, menu = page.menu.map(toDomainMenu))
  }

  def toDomainFilmFrontPage(page: api.NewOrUpdatedFilmFrontPageDTO): Try[domain.FilmFrontPage] = {
    val withoutAboutSubject =
      domain.FilmFrontPage(page.name, Seq(), toDomainMovieThemes(page.movieThemes), page.slideShow, page.article)

    toDomainAboutSubject(page.about) match {
      case Failure(ex)    => Failure(ex)
      case Success(about) => Success(withoutAboutSubject.copy(about = about))
    }
  }

  private def toDomainMovieThemes(themes: Seq[api.NewOrUpdatedMovieThemeDTO]): Seq[MovieTheme] = {
    themes.map(theme => frontpage.MovieTheme(toDomainMovieNames(theme.name), theme.movies))
  }

  private def toDomainMovieNames(names: Seq[api.NewOrUpdatedMovieNameDTO]): Seq[MovieThemeName] = {
    names.map(name => frontpage.MovieThemeName(name.name, name.language))
  }

  private def createImageUrl(id: Long): String   = createImageUrl(id.toString)
  private def createImageUrl(id: String): String = s"${props.RawImageApiUrl}/id/$id"
}
