/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V55__MigrateMarkdownTest extends UnitSuite with TestEnvironment {

  test("That migrating markdown for content works as expected") {
    {
      val testHtml =
        """<section><ndlaembed data-size="full" data-align="" data-caption="Denne captionen har **markdown** så den skal *endres*" data-alt="Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n" data-resource_id="53386" data-resource="image"></ndlaembed></section>"""
      val expectedResult =
        """<section><ndlaembed data-size="full" data-align="" data-caption="Denne captionen har &lt;strong>markdown&lt;/strong> så den skal &lt;em>endres&lt;/em>" data-alt="Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n" data-resource_id="53386" data-resource="image"></ndlaembed></section>""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.fixCaption(testHtml)
      result should be(expectedResult)
    }
    {
      val testHtml =
        """<section><ndlaembed data-size="full" data-align="" data-caption="Denne captionen blir ikkje endra fordi den ikkje har markdown" data-alt="Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n" data-resource_id="53386" data-resource="image"></ndlaembed></section>"""
      val expectedResult =
        """<section><ndlaembed data-size="full" data-align="" data-caption="Denne captionen blir ikkje endra fordi den ikkje har markdown" data-alt="Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n" data-resource_id="53386" data-resource="image"></ndlaembed></section>""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.fixCaption(testHtml)
      result should be(expectedResult)
    }

  }

  test("That migrating markdown for introduction works as expected") {
    {
      val testMarkdown =
        """Dette er en ingress med markdown.
          |Her er et linjeskift.
          |
          |Her er et nytt avsnitt""".stripMargin
      val expectedResult =
        """<p>Dette er en ingress med markdown.<br>Her er et linjeskift.</p>
          |<p>Her er et nytt avsnitt</p>""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.convertMarkdown(testMarkdown)
      result should be(expectedResult)
    }
    {
      val testMarkdown =
        """Dette er en ingress uten markdown. Her er det kun ett langt avsnitt.""".stripMargin
      val expectedResult =
        """Dette er en ingress uten markdown. Her er det kun ett langt avsnitt.""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.convertMarkdown(testMarkdown)
      result should be(expectedResult)
    }
  }
}
