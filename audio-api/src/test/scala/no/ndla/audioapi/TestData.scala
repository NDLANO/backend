/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi

import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType, Copyright, SearchSettings}
import no.ndla.audioapi.model.domain
import org.joda.time.DateTime

import java.util.Date

object TestData {

  val today: DateTime = new DateTime().minusDays(1)
  val yesterday: DateTime = new DateTime()

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    language = None,
    license = None,
    page = None,
    pageSize = None,
    sort = Sort.ByTitleAsc,
    shouldScroll = false,
    audioType = None,
    seriesFilter = None
  )

  val sampleCopyright: Copyright = domain.Copyright(
    license = "CC-BY-4.0",
    origin = Some("origin"),
    creators = Seq(domain.Author("originator", "ole")),
    processors = Seq(domain.Author("processor", "dole")),
    rightsholders = Seq(domain.Author("rightsholder", "doffen")),
    agreementId = None,
    validFrom = None,
    validTo = None
  )

  val sampleAudio: AudioMetaInformation = domain.AudioMetaInformation(
    id = Some(1),
    revision = Some(1),
    titles = Seq(domain.Title("Tittel", "nb")),
    filePaths = Seq(domain.Audio("somepath.mp3", "audio/mpeg", 1024, "nb")),
    copyright = sampleCopyright,
    tags = Seq(domain.Tag(Seq("Some", "Tags"), "nb")),
    updatedBy = "someuser",
    updated = new Date(),
    created = new Date(),
    podcastMeta = Seq.empty,
    audioType = AudioType.Standard,
    manuscript = Seq.empty,
    seriesId = None,
    series = None
  )

  val EpisodelessSampleSeries: domain.Series = domain.Series(
    id = 1,
    revision = 1,
    episodes = None,
    title = Seq(domain.Title("SERIE", "nb")),
    description = Seq(domain.Description("SERIE DESCRIPTION", "nb")),
    coverPhoto = domain.CoverPhoto(imageId = "2", altText = "mainalt"),
    updated = today,
    created = yesterday,
  )

  val samplePodcast: AudioMetaInformation = domain.AudioMetaInformation(
    id = Some(1),
    revision = Some(1),
    titles = Seq(domain.Title("Min kule podcast episode", "nb")),
    filePaths = Seq(domain.Audio("somecast.mp3", "audio/mpeg", 1024, "nb")),
    copyright = sampleCopyright,
    tags = Seq(domain.Tag(Seq("PODCAST", "påddkæst"), "nb")),
    updatedBy = "someuser",
    updated = new Date(),
    created = new Date(),
    podcastMeta = Seq(
      domain.PodcastMeta(
        introduction = "Intro",
        coverPhoto = domain.CoverPhoto(imageId = "1", altText = "alt"),
        language = "nb"
      )
    ),
    audioType = AudioType.Podcast,
    manuscript = Seq.empty,
    seriesId = Some(1),
    series = Some(EpisodelessSampleSeries)
  )

  val SampleSeries: domain.Series = domain.Series(
    id = 1,
    revision = 1,
    episodes = Some(Seq(samplePodcast)),
    title = Seq(domain.Title("SERIE", "nb")),
    description = Seq(domain.Description("SERIE DESCRIPTION", "nb")),
    coverPhoto = domain.CoverPhoto(imageId = "2", altText = "mainalt"),
    updated = today,
    created = yesterday
  )
}
