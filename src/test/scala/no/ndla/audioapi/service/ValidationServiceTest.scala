package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.{AudioType, CoverPhoto, PodcastMeta}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = spy(new ValidationService)

  val audioType = AudioType.Podcast
  val enCoverPhoto = mock[CoverPhoto]
  val nbCoverPhoto = mock[CoverPhoto]
  val meta = Seq(PodcastMeta("intro", enCoverPhoto, "en"), PodcastMeta("intro", nbCoverPhoto, "nb"))

  test("validatePodcastMeta does only evaluate cover photo of chosen language") {

    doReturn(None).when(validationService).validateNonEmpty(any, any)
    doReturn(Seq.empty).when(validationService).validatePodcastCoverPhoto(any[String], any[CoverPhoto])
    validationService.validatePodcastMeta(audioType, meta, Some("en"))

    verify(validationService, times(1)).validatePodcastCoverPhoto(any, any)
    verify(validationService, times(2)).validateNonEmpty(any, any)

  }

}
