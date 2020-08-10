/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

abstract class UnitSuite
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with BeforeAndAfterAll
    with BeforeAndAfterEach
