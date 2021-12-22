/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.{AudioType, CoverPhoto, PodcastMeta, Tag}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

import java.awt.image.BufferedImage

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService: ValidationService = spy(new ValidationService)

  override def beforeEach(): Unit = {
    reset(validationService)
  }

  val audioType: AudioType.Value = AudioType.Podcast
  val enCoverPhoto: CoverPhoto = CoverPhoto("1", "alt")
  val nbCoverPhoto: CoverPhoto = CoverPhoto("2", "alt")
  val meta = Seq(PodcastMeta("intro", enCoverPhoto, "en"), PodcastMeta("intro", nbCoverPhoto, "nb"))

  test("validatePodcastMeta is empty when cover photo is squared") {
    val enImageMock = mock[BufferedImage]
    val nbImageMock = mock[BufferedImage]

    when(enImageMock.getWidth).thenReturn(1500)
    when(enImageMock.getHeight).thenReturn(1500)
    when(converterService.getPhotoUrl(enCoverPhoto)).thenReturn("http://test.url/1")
    when(converterService.getPhotoUrl(nbCoverPhoto)).thenReturn("http://test.url/2")
    doReturn(enImageMock).when(validationService).readImage("http://test.url/1")
    doReturn(nbImageMock).when(validationService).readImage("http://test.url/2")
    val result = validationService.validatePodcastMeta(audioType, meta, Some("en"))

    result should be(Seq.empty)

  }

  test("validatePodcastMeta is not empty when cover photo is not squared") {
    val enImageMock = mock[BufferedImage]
    val nbImageMock = mock[BufferedImage]

    when(enImageMock.getWidth).thenReturn(1500)
    when(enImageMock.getHeight).thenReturn(1600)
    when(converterService.getPhotoUrl(enCoverPhoto)).thenReturn("http://test.url/1")
    when(converterService.getPhotoUrl(nbCoverPhoto)).thenReturn("http://test.url/2")
    doReturn(enImageMock).when(validationService).readImage("http://test.url/1")
    doReturn(nbImageMock).when(validationService).readImage("http://test.url/2")
    val result = validationService.validatePodcastMeta(audioType, meta, Some("en"))

    result.length should be(1)

  }

  test("validateLanguage approves all kinds of languages") {
    val nbTag = Tag(Seq("Tag1", "Tag2"), "nb")
    val nnTag = Tag(Seq("Tag1", "Tag2"), "nn")
    val undTag = Tag(Seq("Tag1", "Tag2"), "und")
    val result = validationService.validateTags(Seq(nbTag, nnTag, undTag), Seq.empty)

    result.length should be(0)
  }

  test("validateLanguage denies unknown") {
    val undTag = Tag(Seq("Tag1", "Tag2"), "unknown")
    val result = validationService.validateTags(Seq(undTag), Seq.empty)

    result.length should be(1)
  }

  test("validateLanguage approves language without languageAnalzer") {
    val undTag = Tag(Seq("Tag1", "Tag2"), "mix") // Mixtepec Mixtec
    val result = validationService.validateTags(Seq(undTag), Seq.empty)

    result.length should be(0)
  }

}
