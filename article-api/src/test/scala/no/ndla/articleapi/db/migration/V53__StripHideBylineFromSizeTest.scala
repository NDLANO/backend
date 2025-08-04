/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V53__StripHideBylineFromSizeTest extends UnitSuite with TestEnvironment {
  test("That hide-byline is stripped from size") {
    val migration = new V53__StripHideBylineFromSize
    val full      =
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>"""
    val medium =
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="medium" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>"""

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full-hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="medium-hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(medium): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full---" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full---hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full-hide-byline--hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="medium-hide-byline--hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(medium): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="full---------hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="fullwidth-hide-byline" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="fullwidth" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

    migration.convertContent(
      """<section><ndlaembed data-resource="image" data-resource_id="123" data-alt="Alt" data-caption="Caption" data-size="fullbredde" data-hide-byline="true" data-url="https://api.test.ndla.no/image-api/v2/images/123"></ndlaembed></section>""",
      "nb"
    ) should be(full): Unit

  }

}
