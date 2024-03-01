/*
 * Part of NDLA audio-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V19__MigrateMarkdownTest extends UnitSuite with TestEnvironment {
  val migration = new V19__MigrateMarkdown

  test("migration should update to new manuscript format") {
    {
      val old =
        s"""{"podcastMeta":[{"header":"Header","introduction":"Intro","coverPhoto":{"imageId":"5","altText":"Alt"},"language":"nb"}],"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}],"manuscript":[{"manuscript":"Intervjuer:
           |
           |Velkommen til intervjuet med Kari Nordmann. Kari er en av NDLAs mest populære forfattere og har skrevet mange artikler og bøker.
           |
           |Kari Nordmann: Hei, takk for at jeg får være med.","language":"nb"}]}""".stripMargin
      val expected =
        s"""{"podcastMeta":[{"header":"Header","introduction":"Intro","coverPhoto":{"imageId":"5","altText":"Alt"},"language":"nb"}],"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}],"manuscript":[{"manuscript":"<p>Intervjuer:</p><p>Velkommen til intervjuet med Kari Nordmann. Kari er en av NDLAs mest populære forfattere og har skrevet mange artikler og bøker.</p><p>Kari Nordmann: Hei, takk for at jeg får være med.</p>","language":"nb"}]}""".stripMargin
      migration.convertDocument(old) should equal(expected)
    }
  }
}
