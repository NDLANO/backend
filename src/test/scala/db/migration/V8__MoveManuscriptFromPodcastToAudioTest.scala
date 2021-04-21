/*
 * Part of NDLA audio-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V8__MoveManuscriptFromPodcastToAudioTest extends UnitSuite with TestEnvironment {
  val migration = new V8__MoveManuscriptFromPodcastToAudio

  test("migration should update to new manuscript format") {
    {
      val old =
        s"""{"podcastMeta":[{"header":"Header","introduction":"Intro","manuscript":"test","coverPhoto":{"imageId":"5","altText":"Alt"},"language":"nb"}],"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"podcastMeta":[{"header":"Header","introduction":"Intro","coverPhoto":{"imageId":"5","altText":"Alt"},"language":"nb"}],"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}],"manuscript":[{"manuscript":"test","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)
    }
  }
}
