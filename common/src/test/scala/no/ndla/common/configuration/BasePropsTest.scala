/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

import no.ndla.common.auth.Permission
import no.ndla.testbase.UnitTestSuiteBase

class BasePropsTest extends UnitTestSuiteBase {
  class TestProps extends BaseProps {
    override def ApplicationPort: Int             = 1234
    override def ApplicationName: String          = "testapp"
    override val ndlaAuth0Scopes: Seq[Permission] = Seq.empty
    def requiredProp: Prop[String]                = prop("NDLA_SOME_REQUIRED_TEST_PROP")
    def someOtherRequiredProp: Prop[String]       = prop("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
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
    val result: Prop[Int] = props.propMap {
      props.requiredProp
    } { x =>
      x.toInt + 1
    }
    val value = result.unsafeGet
    value should be(5556)
    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
    System.clearProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

  test("That failed mapping props to int fails on unsafeGet") {
    val props = new TestProps
    System.setProperty("NDLA_SOME_REQUIRED_TEST_PROP", "hei")
    System.setProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP", "some-other-test-result")
    val result: Prop[Int] = props.propMap {
      props.requiredProp
    } { x =>
      x.toInt + 1
    }
    val ex = intercept[EnvironmentNotFoundException] {
      result.unsafeGet
    }
    ex.getMessage should be("Unable to load property NDLA_SOME_REQUIRED_TEST_PROP")
    ex.getCause.getMessage should be("For input string: \"hei\"")
    ex.getCause shouldBe a[NumberFormatException]
    System.clearProperty("NDLA_SOME_REQUIRED_TEST_PROP")
    System.clearProperty("NDLA_SOME_OTHER_REQUIRED_TEST_PROP")
  }

  class PrefixedTestProps extends BaseProps {
    override def ApplicationPort: Int             = 1234
    override def ApplicationName: String          = "article-api"
    override val ndlaAuth0Scopes: Seq[Permission] = Seq.empty
    def value: Prop[String]                       = prefixedProp("SOME_TEST_VAR")
    def valueWithDefault: Prop[String]            = prefixedPropOrElse("OTHER_TEST_VAR", "fallback")
    def intValueWithDefault: Int                  = prefixedIntPropOrElse("INT_TEST_VAR", 42)
  }

  test("prefixedProp prefers the app-prefixed env var over the unprefixed one") {
    val props = new PrefixedTestProps
    System.setProperty("ARTICLE_API_SOME_TEST_VAR", "from-prefix")
    System.setProperty("SOME_TEST_VAR", "from-shared")
    try
      props.value.toString should be("from-prefix")
    finally {
      System.clearProperty("ARTICLE_API_SOME_TEST_VAR")
      System.clearProperty("SOME_TEST_VAR")
    }
  }

  test("prefixedProp falls back to the unprefixed env var when the prefix is absent") {
    val props = new PrefixedTestProps
    System.setProperty("SOME_TEST_VAR", "from-shared")
    try props.value.toString should be("from-shared")
    finally System.clearProperty("SOME_TEST_VAR")
  }

  test("prefixedProp fails throwIfFailedProps when neither variant is set") {
    val props = new PrefixedTestProps
    props.value // register the prop as failed
    intercept[EnvironmentNotFoundException] {
      props.throwIfFailedProps()
    }
  }

  test("prefixedPropOrElse uses the supplied default when neither variant is set") {
    val props = new PrefixedTestProps
    props.valueWithDefault.toString should be("fallback")
  }

  test("prefixedIntPropOrElse honours the prefix and parses Int") {
    val props = new PrefixedTestProps
    System.setProperty("ARTICLE_API_INT_TEST_VAR", "7")
    try props.intValueWithDefault should be(7)
    finally System.clearProperty("ARTICLE_API_INT_TEST_VAR")
  }
}
