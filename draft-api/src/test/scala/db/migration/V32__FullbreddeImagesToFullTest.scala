/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

import scala.util.Success

class V32__FullbreddeImagesToFullTest extends UnitSuite with TestEnvironment {

  test("Images in visualElement and content should be converted correctly") {

    val beforeArticle =
      """{"visualElement":[{"resource":"<embed data-size=\"fullbredde-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nb"},{"resource":"<embed data-size=\"fullbredde\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nn"}],"content":[{"content":"<section><embed data-size=\"fullbredde-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"fullbredde\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nb"},{"content":"<section><embed data-size=\"fullbredde-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"fullbredde\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nn"}]}"""
    val expectedArticle =
      """{"visualElement":[{"resource":"<embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nb"},{"resource":"<embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nn"}],"content":[{"content":"<section><embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nb"},{"content":"<section><embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nn"}]}"""

    val migration = new V32__FullbreddeImagesToFull
    migration.convertArticleUpdate(beforeArticle) should be(expectedArticle)
  }

  test("Already converted content should not be broken") {

    val beforeArticle =
      """{"visualElement":[{"resource":"<embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nb"},{"resource":"<embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\">","language":"nn"}],"content":[{"content":"<section><embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nb"},{"content":"<section><embed data-size=\"full-hide-byline\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"><embed data-size=\"full\" data-align=\"\" data-caption=\"\" data-alt=\"Ein ung mann sit på kanten av senga og støttar hovudet i den eine handa. Foto.\n\" data-resource_id=\"53386\" data-resource=\"image\"></section>","language":"nn"}]}"""

    val migration = new V32__FullbreddeImagesToFull
    migration.convertArticleUpdate(beforeArticle) should be(beforeArticle)
  }
}
