/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

import no.ndla.testbase.UnitTestSuiteBase

class BasePropsTest extends UnitTestSuiteBase {
  class TestProps extends BaseProps {
    override def ApplicationPort: Int       = 1234
    override def ApplicationName: String    = "testapp"
    def requiredProp: Prop[String]          = prop("NDLA_SOME_REQUIRED_TEST_PROP")
    def someOtherRequiredProp: Prop[String] = prop("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

  test("That props works if system property is set") {
    val props = new TestProps
    System.setProperty("NDLA_SOME_REQUIRED_TEST_PROP", "some-test-result")
    System.setProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP", "some-other-test-result")
    val result: String = props.requiredProp
    result should be("some-test-result")
    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
    System.clearProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

  test("That props crashes if system property is not set") {
    val props = new TestProps

    System.setProperty("NDLA_SOME_REQUIRED_TEST_PROP", "some-test-result")

    intercept[EnvironmentNotFoundException] {
      props.requiredProp.toString
      props.someOtherRequiredProp.toString
    }

    intercept[EnvironmentNotFoundException] {
      props.throwIfFailedProps()
    }

    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
  }

  test("That mapping props to int works") {
    val props = new TestProps
    System.setProperty("NDLA_SOME_REQUIRED_TEST_PROP", "5555")
    System.setProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP", "some-other-test-result")
    val result: Prop[Int] = props.propMap { props.requiredProp } { x => x.toInt + 1 }
    val value             = result.unsafeGet
    value should be(5556)
    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
    System.clearProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

  test("That failed mapping props to int fails on unsafeGet") {
    val props = new TestProps
    System.setProperty("NDLA_SOME_REQUIRED_TEST_PROP", "hei")
    System.setProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP", "some-other-test-result")
    val result: Prop[Int] = props.propMap { props.requiredProp } { x => x.toInt + 1 }
    val ex                = intercept[EnvironmentNotFoundException] {
      result.unsafeGet
    }
    ex.getMessage should be("Unable to load property NDLA_SOME_REQUIRED_TEST_PROP")
    ex.getCause.getMessage should be("For input string: \"hei\"")
    ex.getCause shouldBe a[NumberFormatException]
    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
    System.clearProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

}
