/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class TextValidatorTest extends UnitSuite with TestEnvironment {
  import props.AllowedHtmlTags

  var allowedHtmlValidator: TextValidator = _
  var noHtmlValidator: TextValidator      = _

  override def beforeEach(): Unit = {
    allowedHtmlValidator = new TextValidator(allowHtml = true)
    noHtmlValidator = new TextValidator(allowHtml = false)
  }

  test("That TextValidator allows all tags in AllowedHtmlTags tags") {
    AllowedHtmlTags.foreach(tag => {
      val starttext = s"<$tag>This is text with $tag"
      val text      = starttext + (if (tag.equals("br")) "" else s"</$tag>")
      allowedHtmlValidator.validate("path1.path2", text) should equal(None)
    })
  }

  test("That TextValidator does not allow tags outside BaiscHtmlTags") {
    val illegalTag = "a"
    AllowedHtmlTags.contains(illegalTag) should be(right = false)

    val text = s"<$illegalTag>This is text with $illegalTag</$illegalTag>"

    val validationMessage = allowedHtmlValidator.validate("path1.path2", text)
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("path1.path2")
    validationMessage.get.message should equal(allowedHtmlValidator.IllegalContentInBasicText)
  }

  test("That TextValidator does not allow any html in plain text") {
    val textWithHtml = "<strong>This is text with html</strong>"
    val validationMessage =
      noHtmlValidator.validate("path1.path2", textWithHtml)
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("path1.path2")
    validationMessage.get.message should equal(noHtmlValidator.IllegalContentInPlainText)
  }

  test("That TextValidator allows plain text in plain text") {
    noHtmlValidator.validate("path1", "This is plain text") should be(None)
  }
}
