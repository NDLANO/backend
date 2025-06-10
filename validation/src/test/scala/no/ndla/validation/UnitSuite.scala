/*
 * Part of NDLA validation
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import org.scalatest.*
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
