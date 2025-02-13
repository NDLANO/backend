/*
 * Part of NDLA language
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language

import org.scalatest._
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

abstract class UnitSuite
    extends AsyncFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with BeforeAndAfterAll
    with BeforeAndAfterEach
