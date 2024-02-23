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
        """Dette er en ingress med avsnitt som skal konverteres.
          |Her er et linjeskift.
          |
          |Her er et nytt avsnitt""".stripMargin
      val expectedResult =
        """<p>Dette er en ingress med avsnitt som skal konverteres.
          |Her er et linjeskift.</p>
          |<p>Her er et nytt avsnitt</p>""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.convertMarkdown(testMarkdown)
      result should be(expectedResult)
    }
    {
      // format off
      val testMarkdown =
        """Dette er en ingress med markdown.
        |Også her et linjeskift.
        |
        |""".stripMargin + "Linje med to mellomrom  \n" +
        """som blir til en br
        |
        |* liste
        |* skal
        |* ikkje
        |* rendres
        |1. heller
        |2. ikkje
        |3. nummererte
        ||tabell | med | markdown | skal | ignoreres |
        ||den | skal | ignoreres | sa | eg |
        |
        |`kodeblokk er ok`
        |
        |Her er et nytt avsnitt""".stripMargin
      // format on
      val expectedResult =
        """<p>Dette er en ingress med markdown.
          |Også her et linjeskift.</p>
          |<p>Linje med to mellomrom<br>som blir til en br</p>
          |<p>* liste
          |* skal
          |* ikkje
          |* rendres
          |1. heller
          |2. ikkje
          |3. nummererte
          ||tabell | med | markdown | skal | ignoreres |
          ||den | skal | ignoreres | sa | eg |</p>
          |<p><code>kodeblokk er ok</code></p>
          |<p>Her er et nytt avsnitt</p>""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.convertMarkdown(testMarkdown)
      result should be(expectedResult)
    }
    {
      val testMarkdown =
        """Dette er en ingress uten markdown. Her er det kun ett langt avsnitt. Den lar vi være slik den er!""".stripMargin
      val expectedResult =
        """Dette er en ingress uten markdown. Her er det kun ett langt avsnitt. Den lar vi være slik den er!""".stripMargin
      val migration = new V55__MigrateMarkdown
      val result    = migration.convertMarkdown(testMarkdown)
      result should be(expectedResult)
    }
  }
}
