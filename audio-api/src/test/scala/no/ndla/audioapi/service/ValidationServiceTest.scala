/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.{AudioType, CoverPhoto, PodcastMeta}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.mapping.License.CC_BY
import org.mockito.Mockito.{doReturn, reset, spy, when}

import java.awt.image.BufferedImage

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService: ValidationService = spy(new ValidationService)

  override def beforeEach(): Unit = {
    reset(validationService)
  }

  val audioType: AudioType.Value = AudioType.Podcast
  val enCoverPhoto: CoverPhoto   = CoverPhoto("1", "alt")
  val nbCoverPhoto: CoverPhoto   = CoverPhoto("2", "alt")
  val meta: Seq[PodcastMeta] = Seq(PodcastMeta("intro", enCoverPhoto, "en"), PodcastMeta("intro", nbCoverPhoto, "nb"))

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
    val nbTag  = Tag(Seq("Tag1", "Tag2"), "nb")
    val nnTag  = Tag(Seq("Tag1", "Tag2"), "nn")
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

  test("validateCopyright should succeed if a copyright holder is provided") {
    val copyright =
      Copyright(CC_BY.toString, None, Seq(Author("artist", "test")), Seq.empty, Seq.empty, None, None, false)
    val result = validationService.validateCopyright(copyright)
    result.length should be(0)
  }

  test("validateCopyright should fail if no copyright holders are provided") {
    val copyright = Copyright(CC_BY.toString, None, Seq.empty, Seq.empty, Seq.empty, None, None, false)
    val result    = validationService.validateCopyright(copyright)
    result.length should be(1)
    result should be(
      Seq(
        ValidationMessage(
          "license.license",
          s"At least one copyright holder is required when license is ${CC_BY.toString}"
        )
      )
    )
  }
}
