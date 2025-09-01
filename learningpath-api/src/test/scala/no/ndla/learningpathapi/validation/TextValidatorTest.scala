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
  var allowedHtmlValidator: TextValidator = scala.compiletime.uninitialized
  var noHtmlValidator: TextValidator      = scala.compiletime.uninitialized

  override def beforeEach(): Unit = {
    allowedHtmlValidator = new TextValidator(allowHtml = true)
    noHtmlValidator = new TextValidator(allowHtml = false)
  }

  test("That TextValidator allows all tags in AllowedHtmlTags tags") {
    props.AllowedHtmlTags.foreach(tag => {
      val starttext = s"<$tag>This is text with $tag"
      val text      = starttext + (if (tag.equals("br")) "" else s"</$tag>")
      allowedHtmlValidator.validate("path1.path2", text) should equal(None)
    })
  }

  test("That TextValidator allows links in html") {
    allowedHtmlValidator.validate(
      "path1",
      """<p>This is plain text with a <a href="https://ndla.no" target="_blank" rel="noopener noreferrer">link</a></p>"""
    ) should equal(None)
  }

  test("That TextValidator does not allow tags outside BasicHtmlTags") {
    val illegalTag = "aside"
    props.AllowedHtmlTags.contains(illegalTag) should be(right = false)

    val text = s"<$illegalTag>This is text with $illegalTag</$illegalTag>"

    val validationMessage = allowedHtmlValidator.validate("path1.path2", text)
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("path1.path2")
    validationMessage.get.message should equal(allowedHtmlValidator.IllegalContentInBasicText)
  }

  test("That TextValidator does not allow any html in plain text") {
    val textWithHtml      = "<strong>This is text with html</strong>"
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
