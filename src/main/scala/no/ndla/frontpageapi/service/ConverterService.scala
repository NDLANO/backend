/*
 * Part of NDLA frontpage_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.FrontpageApiProperties.{BrightcoveAccountId, BrightcovePlayer, RawImageApiUrl}
import no.ndla.frontpageapi.model.domain.Errors.LanguageNotFoundException
import no.ndla.frontpageapi.model.domain.Language._
import no.ndla.frontpageapi.model.domain._
import no.ndla.frontpageapi.model.{api, domain}

import scala.util.{Failure, Success, Try}

object ConverterService {

  def toApiFrontPage(page: domain.FrontPageData): api.FrontPageData =
    api.FrontPageData(page.topical, page.categories.map(toApiSubjectCollection))

  private def toApiSubjectCollection(coll: domain.SubjectCollection): api.SubjectCollection =
    api.SubjectCollection(coll.name, coll.subjects.map(sf => api.SubjectFilters(sf.id, sf.filters)))

  private def toApiBannerImage(banner: domain.BannerImage): api.BannerImage =
    api.BannerImage(banner.mobileImageId.map(createImageUrl),
                    banner.mobileImageId,
                    createImageUrl(banner.desktopImageId),
                    banner.desktopImageId)

  def toApiFilmFrontPage(page: domain.FilmFrontPageData, language: Option[String]): api.FilmFrontPageData = {
    api.FilmFrontPageData(page.name,
                          toApiAboutFilmSubject(page.about, language),
                          toApiMovieThemes(page.movieThemes, language),
                          page.slideShow)
  }

  private def toApiAboutFilmSubject(aboutSeq: Seq[domain.AboutSubject],
                                    language: Option[String]): Seq[api.AboutFilmSubject] = {
    val filteredAboutSeq = language match {
      case Some(lang) => aboutSeq.filter(about => about.language == lang)
      case None       => aboutSeq
    }
    filteredAboutSeq.map(about =>
      api.AboutFilmSubject(about.title, about.description, toApiVisualElement(about.visualElement), about.language))
  }

  private def toApiMovieThemes(themes: Seq[domain.MovieTheme], language: Option[String]): Seq[api.MovieTheme] = {
    themes.map(theme => api.MovieTheme(toApiMovieName(theme.name, language), theme.movies))
  }

  private def toApiMovieName(names: Seq[domain.MovieThemeName], language: Option[String]): Seq[api.MovieThemeName] = {
    val filteredNames = language match {
      case Some(lang) => names.filter(name => name.language == lang)
      case None       => names
    }

    filteredNames.map(name => api.MovieThemeName(name.name, name.language))
  }

  def toApiSubjectPage(sub: domain.SubjectFrontPageData,
                       language: String,
                       fallback: Boolean = false): Try[api.SubjectPageData] = {
    if (sub.supportedLanguages.contains(language) || fallback) {
      Success(
        api.SubjectPageData(
          sub.id.get,
          sub.name,
          sub.filters,
          sub.layout.toString,
          sub.twitter,
          sub.facebook,
          toApiBannerImage(sub.bannerImage),
          toApiAboutSubject(findByLanguageOrBestEffort(sub.about, language)),
          toApiMetaDescription(findByLanguageOrBestEffort(sub.metaDescription, language)),
          sub.topical,
          sub.mostRead,
          sub.editorsChoices,
          sub.latestContent,
          sub.goTo,
          sub.supportedLanguages
        )
      )
    } else {
      Failure(
        LanguageNotFoundException(s"The subjectpage with id ${sub.id.get} and language $language was not found",
                                  sub.supportedLanguages)
      )
    }
  }

  private def toApiAboutSubject(about: Option[domain.AboutSubject]): Option[api.AboutSubject] = {
    about
      .map(about => api.AboutSubject(about.title, about.description, toApiVisualElement(about.visualElement)))
  }

  private def toApiMetaDescription(meta: Option[domain.MetaDescription]): Option[String] = {
    meta
      .map(_.metaDescription)
  }

  private def toApiVisualElement(visual: domain.VisualElement): api.VisualElement = {
    val url = visual.`type` match {
      case VisualElementType.Image => createImageUrl(visual.id.toLong)
      case VisualElementType.Brightcove =>
        s"https://players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayer}_default/index.html?videoId=${visual.id}"
    }
    api.VisualElement(visual.`type`.toString, url, visual.alt)
  }

  def toDomainSubjectPage(id: Long, subject: api.NewSubjectFrontPageData): Try[domain.SubjectFrontPageData] =
    toDomainSubjectPage(subject).map(_.copy(id = Some(id)))

  private def toDomainBannerImage(banner: api.NewOrUpdateBannerImage): domain.BannerImage =
    domain.BannerImage(banner.mobileImageId, banner.desktopImageId)

  def toDomainSubjectPage(subject: api.NewSubjectFrontPageData): Try[domain.SubjectFrontPageData] = {
    val withoutAboutSubject = domain.SubjectFrontPageData(
      None,
      subject.name,
      subject.filters,
      toDomainLayout(subject.layout),
      subject.twitter,
      subject.facebook,
      toDomainBannerImage(subject.banner),
      Seq(),
      toDomainMetaDescription(subject.metaDescription),
      subject.topical,
      subject.mostRead,
      subject.editorsChoices,
      subject.latestContent,
      subject.goTo
    )

    toDomainAboutSubject(subject.about) match {
      case Failure(ex)    => Failure(ex)
      case Success(about) => Success(withoutAboutSubject.copy(about = about))
    }
  }

  def toDomainSubjectPage(toMergeInto: domain.SubjectFrontPageData,
                          subject: api.UpdatedSubjectFrontPageData): Try[domain.SubjectFrontPageData] = {

    val domainLayout = subject.layout
      .map(toDomainLayout)

    val domainBannerImage = subject.banner
      .map(toDomainBannerImage)

    val domainMetaDescription = subject.metaDescription.fold(Seq[MetaDescription]())(toDomainMetaDescription)

    val domainAboutSubject =
      subject.about.fold(Seq[AboutSubject]())(about => toDomainAboutSubject(about).getOrElse(Seq()))

    Try(
      toMergeInto.copy(
        id = toMergeInto.id,
        name = subject.name
          .getOrElse(toMergeInto.name),
        filters = subject.filters.orElse(toMergeInto.filters),
        layout = domainLayout.getOrElse(toMergeInto.layout),
        twitter = subject.twitter.orElse(toMergeInto.twitter),
        facebook = subject.facebook.orElse(toMergeInto.facebook),
        bannerImage = domainBannerImage.getOrElse(toMergeInto.bannerImage),
        about = mergeLanguageFields(toMergeInto.about, domainAboutSubject),
        metaDescription = mergeLanguageFields(toMergeInto.metaDescription, domainMetaDescription),
        topical = subject.topical.orElse(toMergeInto.topical),
        mostRead = subject.mostRead.getOrElse(toMergeInto.mostRead),
        editorsChoices = subject.editorsChoices.getOrElse(toMergeInto.editorsChoices),
        latestContent = subject.latestContent.orElse(toMergeInto.latestContent),
        goTo = subject.goTo.getOrElse(toMergeInto.goTo)
      ))
  }

  private def toDomainLayout(layout: String): domain.LayoutType.Value = {
    LayoutType.fromString(layout).get
  }

  private def toDomainAboutSubject(aboutSeq: Seq[api.NewOrUpdatedAboutSubject]): Try[Seq[domain.AboutSubject]] = {
    val seq = aboutSeq.map(
      about =>
        toDomainVisualElement(about.visualElement)
          .map(domain
            .AboutSubject(about.title, about.description, about.language, _)))
    Try(seq.map(_.get))
  }

  private def toDomainMetaDescription(metaSeq: Seq[api.NewOrUpdatedMetaDescription]): Seq[domain.MetaDescription] = {
    metaSeq.map(meta => domain.MetaDescription(meta.metaDescription, meta.language))
  }

  private def toDomainVisualElement(visual: api.NewOrUpdatedVisualElement): Try[domain.VisualElement] =
    VisualElementType
      .fromString(visual.`type`)
      .map(domain.VisualElement(_, visual.id, visual.alt))

  private[service] def mergeLanguageFields[A <: LanguageField](existing: Seq[A], updated: Seq[A]): Seq[A] = {
    val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
    (toKeep ++ updated).filterNot(_.isEmpty)
  }

  def toDomainFrontPage(page: api.FrontPageData): domain.FrontPageData =
    domain.FrontPageData(page.topical, page.categories.map(toDomainSubjectCollection))

  private def toDomainSubjectCollection(coll: api.SubjectCollection): domain.SubjectCollection =
    domain.SubjectCollection(coll.name, coll.subjects.map(sf => domain.SubjectFilters(sf.id, sf.filters)))

  def toDomainFilmFrontPage(page: api.NewOrUpdatedFilmFrontPageData): Try[domain.FilmFrontPageData] = {
    val withoutAboutSubject =
      domain.FilmFrontPageData(page.name, Seq(), toDomainMovieThemes(page.movieThemes), page.slideShow)

    toDomainAboutSubject(page.about) match {
      case Failure(ex)    => Failure(ex)
      case Success(about) => Success(withoutAboutSubject.copy(about = about))
    }
  }

  private def toDomainMovieThemes(themes: Seq[api.NewOrUpdatedMovieTheme]): Seq[domain.MovieTheme] = {
    themes.map(theme => domain.MovieTheme(toDomainMovieNames(theme.name), theme.movies))
  }

  private def toDomainMovieNames(names: Seq[api.NewOrUpdatedMovieName]): Seq[domain.MovieThemeName] = {
    names.map(name => domain.MovieThemeName(name.name, name.language))
  }

  private def createImageUrl(id: Long): String = createImageUrl(id.toString)
  private def createImageUrl(id: String): String = s"$RawImageApiUrl/id/$id"
}
