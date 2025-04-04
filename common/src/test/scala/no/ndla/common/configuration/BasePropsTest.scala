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
    override def ApplicationPort: Int    = 1234
    override def ApplicationName: String = "testapp"
    def requiredProp: Prop               = prop("NDLA_SOME_REQUIRED_TEST_PROP")
    def someOtherRequiredProp: Prop      = prop("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
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

}
