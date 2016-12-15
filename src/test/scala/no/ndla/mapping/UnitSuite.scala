/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

import org.scalatest._
import org.scalatest.mock.MockitoSugar

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll with BeforeAndAfterEach
