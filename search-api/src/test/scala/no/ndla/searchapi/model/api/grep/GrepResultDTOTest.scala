/*
 * Part of NDLA backend.search-api.test
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import no.ndla.searchapi.UnitSuite
import scala.reflect.runtime.universe.*

class GrepResultDTOTest extends UnitSuite {

  test("test that GrepResultDTO has typescript generation for all subclasses") {
    val rtm        = runtimeMirror(getClass.getClassLoader)
    val subclasses = rtm.staticClass("no.ndla.searchapi.model.api.grep.GrepResultDTO").knownDirectSubclasses
    subclasses.size should be(5)
    subclasses.foreach { subclass =>
      val generatedType = GrepResultDTO.typescriptUnionTypes.find(_.name == s"I${subclass.name}")
      if (generatedType.isEmpty)
        fail(
          s"Missing typescript type for ${subclass.name}. Please update `no.ndla.searchapi.model.api.grep.GrepResultDTO.typescriptUnionTypes`"
        )
    }

  }

}
