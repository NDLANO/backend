/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import org.scalatest._
import org.scalatest.mock.MockitoSugar

abstract class UnitSuite
    extends FunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterAll
    with BeforeAndAfterEach
