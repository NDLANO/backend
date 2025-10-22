/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType, SearchSettings}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.api
import no.ndla.common.model.domain.ContributorType
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.mapping.License
import no.ndla.network.tapir.auth.Permission.AUDIO_API_WRITE
import no.ndla.network.tapir.auth.TokenUser

object TestData {

  val today: NDLADate     = NDLADate.now().minusDays(1).withNano(0)
  val yesterday: NDLADate = NDLADate.now().withNano(0)

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    language = None,
    license = None,
    page = None,
    pageSize = None,
    sort = Sort.ByTitleAsc,
    shouldScroll = false,
    audioType = None,
    seriesFilter = None,
    fallback = false,
  )

  val sampleCopyright: Copyright = Copyright(
    license = "CC-BY-4.0",
    origin = Some("origin"),
    creators = Seq(common.Author(ContributorType.Originator, "ole")),
    processors = Seq(common.Author(ContributorType.Processor, "dole")),
    rightsholders = Seq(common.Author(ContributorType.RightsHolder, "doffen")),
    validFrom = None,
    validTo = None,
    false,
  )

  val sampleAudio: AudioMetaInformation = domain.AudioMetaInformation(
    id = Some(1),
    revision = Some(1),
    titles = Seq(common.Title("Tittel", "nb")),
    filePaths = Seq(domain.Audio("somepath.mp3", "audio/mpeg", 1024, "nb")),
    copyright = sampleCopyright,
    tags = Seq(common.Tag(Seq("Some", "Tags"), "nb")),
    updatedBy = "someuser",
    updated = NDLADate.now(),
    created = NDLADate.now(),
    podcastMeta = Seq.empty,
    audioType = AudioType.Standard,
    manuscript = Seq.empty,
    seriesId = None,
    series = None,
  )

  val EpisodelessSampleSeries: domain.Series = domain.Series(
    id = 1,
    revision = 1,
    episodes = None,
    title = Seq(common.Title("SERIE", "nb")),
    description = Seq(domain.Description("SERIE DESCRIPTION", "nb")),
    coverPhoto = domain.CoverPhoto(imageId = "2", altText = "mainalt"),
    updated = today,
    created = yesterday,
    hasRSS = true,
  )

  val samplePodcast: AudioMetaInformation = domain.AudioMetaInformation(
    id = Some(1),
    revision = Some(1),
    titles = Seq(common.Title("Min kule podcast episode", "nb")),
    filePaths = Seq(domain.Audio("somecast.mp3", "audio/mpeg", 1024, "nb")),
    copyright = sampleCopyright,
    tags = Seq(common.Tag(Seq("PODCAST", "påddkæst"), "nb")),
    updatedBy = "someuser",
    updated = NDLADate.now(),
    created = NDLADate.now(),
    podcastMeta = Seq(
      domain.PodcastMeta(
        introduction = "Intro",
        coverPhoto = domain.CoverPhoto(imageId = "1", altText = "alt"),
        language = "nb",
      )
    ),
    audioType = AudioType.Podcast,
    manuscript = Seq.empty,
    seriesId = Some(1),
    series = Some(EpisodelessSampleSeries),
  )

  val SampleSeries: domain.Series = domain.Series(
    id = 1,
    revision = 1,
    episodes = Some(Seq(samplePodcast)),
    title = Seq(common.Title("SERIE", "nb")),
    description = Seq(domain.Description("SERIE DESCRIPTION", "nb")),
    coverPhoto = domain.CoverPhoto(imageId = "2", altText = "mainalt"),
    updated = today,
    created = yesterday,
    hasRSS = true,
  )

  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created: NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val DefaultApiImageMetaInformation: api.AudioMetaInformationDTO = api.AudioMetaInformationDTO(
    1,
    1,
    api.TitleDTO("title", "nb"),
    api.AudioDTO("audio/test.mp3", "audio/mpeg", 1024, "nb"),
    commonApi.CopyrightDTO(
      commonApi.LicenseDTO(License.CC_BY_SA.toString, None, None),
      None,
      Seq(),
      Seq(),
      Seq(),
      None,
      None,
      false,
    ),
    api.TagDTO(Seq("tag"), "nb"),
    Seq("nb"),
    "standard",
    None,
    None,
    None,
    created,
    updated,
  )

  val testUser: TokenUser = TokenUser("ndla54321", Set(AUDIO_API_WRITE), None)
}
