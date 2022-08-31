/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.db.migration.V10__RemoveImageVisualElementsWithoutIds
import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V10__RemoveImageVisualElementsWithoutIdsTest extends UnitSuite with TestEnvironment {
  val migration = new V10__RemoveImageVisualElementsWithoutIds

  test("Image visual elements without id should be removed") {
    val old =
      """{"visualElement":[{"visualElement":"<embed data-resource_id=\"\" data-resource=\"image\" />","language":"nb"},{"visualElement":"<embed data-resource_id=\"2\" data-resource=\"image\" />","language":"nn"}]}"""
    val expected =
      """{"visualElement":[{"visualElement":"<embed data-resource_id=\"2\" data-resource=\"image\" />","language":"nn"}]}"""

    migration.convertToNewConcept(old, 1) should be(expected)
  }

}
