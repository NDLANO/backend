/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.{AudioType, CoverPhoto, PodcastMeta}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

import java.awt.image.BufferedImage

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = spy(new ValidationService)

  override def beforeEach(): Unit = {
    reset(validationService)
  }

  val audioType = AudioType.Podcast
  val enCoverPhoto = CoverPhoto("1", "alt")
  val nbCoverPhoto = CoverPhoto("2", "alt")
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

}
