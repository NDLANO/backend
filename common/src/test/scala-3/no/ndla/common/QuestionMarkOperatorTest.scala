/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common

import no.ndla.common.implicits._
import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.{Failure, Success, Try}

class QuestionMarkOperatorTest extends UnitTestSuite {

  val testException              = new RuntimeException("Bad method")
  def failingMethod: Try[Int]    = Failure(testException)
  def succeedingMethod: Try[Int] = Success(1)

  def someMethod: Option[Int] = Some(1)
  def noneMethod: Option[Int] = None

  test("That question mark operator works on success for try methods") {
    def tryMethod(): Try[String] = permitTry {
      val someData = succeedingMethod.?
      Success(someData.toString)
    }
    tryMethod() should be(Success("1"))
  }

  test("That question mark operator works on failure for try methods") {
    def tryMethod(): Try[String] = permitTry {
      val someData = failingMethod.?
      Success(someData.toString)
    }
    tryMethod() should be(Failure(testException))
  }

//  test("That question mark operator works on some for option methods") {
//    def optMethod(): Option[String] = permitTry {
//      val someData = someMethod.?
//      Some(someData.toString)
//    }
//    optMethod() should be(Some("1"))
//  }
//
//  test("That question mark operator works on none for option methods") {
//    def optMethod(): Option[String] = permitTry {
//      val someData = noneMethod.?
//      Some(someData.toString)
//    }
//    optMethod() should be(None)
//  }

}
