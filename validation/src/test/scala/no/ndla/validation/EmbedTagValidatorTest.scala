/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.mapping.UnitSuite
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.validation.TagRules.Condition

class EmbedTagValidatorTest extends UnitSuite {
  val embedTagValidator = new TagValidator()

  private def childCountValidationMessage(fieldName: String) =
    Seq(
      ValidationMessage(
        fieldName,
        "Parent condition block is invalid. " +
          "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1')."))

  private def generateAttributes(attrs: Map[String, String]): String = {
    attrs.toList.sortBy(_._1.toString).map { case (key, value) => s"""$key="$value"""" }.mkString(" ")
  }

  private def generateTagWithAttrs(attrs: Map[TagAttributes.Value, String]): String = {
    val strAttrs = attrs map { case (k, v) => k.toString -> v }
    s"""<$ResourceHtmlEmbedTag ${generateAttributes(strAttrs)} />"""
  }

  private def findErrorByMessage(validationMessages: Seq[ValidationMessage], partialMessage: String) =
    validationMessages.find(_.message.contains(partialMessage))

  test("validate should return an empty sequence if input is not an embed tag or an html tag with attributes") {
    embedTagValidator.validate("content", "<h1>hello</h1>") should equal(Seq())
  }

  test("validate should return a validation error if input is a html tag with that should not have attributes") {
    val res = embedTagValidator.validate("content", "<h1 test='test'>hello</h1>")
    findErrorByMessage(res, "An HTML tag 'h1' contains an illegal attribute(s) 'test'.").size should be(1)
  }

  test("validate should return validation error if embed tag uses illegal attributes") {
    val attrs = generateAttributes(
      Map(TagAttributes.DataResource.toString -> ResourceType.ExternalContent.toString,
          TagAttributes.DataUrl.toString -> "google.com",
          "illegalattr" -> "test"))

    val res = embedTagValidator.validate("content", s"""<$ResourceHtmlEmbedTag $attrs />""")
    findErrorByMessage(res, "illegal attribute(s) 'illegalattr'").size should be(1)
  }

  test("validate should return validation error if an attribute contains HTML") {
    val tag = generateTagWithAttrs(
      Map(TagAttributes.DataResource -> ResourceType.ExternalContent.toString,
          TagAttributes.DataUrl -> "<i>google.com</i>"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, "contains attributes with HTML: data-url").size should be(1)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=image") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.Image.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Image} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return not validation error if embed tag misses moved required to optional") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataResource_Id -> "1234",
        TagAttributes.DataSize -> "fullbredde",
        TagAttributes.DataAlign -> "",
        TagAttributes.DataAlt -> "alttext"
      ))
    val res = embedTagValidator.validate("content",
                                         tag,
                                         requiredToOptional = Map("image" -> Seq(TagAttributes.DataCaption.toString)))
    res should be(Seq.empty)
  }

  test("validate should return no validation errors if image embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataResource_Id -> "1234",
        TagAttributes.DataSize -> "fullbredde",
        TagAttributes.DataAlt -> "alternative text",
        TagAttributes.DataCaption -> "here is a rabbit",
        TagAttributes.DataAlign -> "left"
      ))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=audio") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.Audio.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Audio} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if audio embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Audio.toString,
        TagAttributes.DataResource_Id -> "1234",
        TagAttributes.DataCaption -> "",
        TagAttributes.DataType -> "standard"
      ))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=h5p") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.H5P.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.H5P} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if h5p embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.H5P.toString,
        TagAttributes.DataPath -> "/h5p/embed/1234"
      ))
    val t = embedTagValidator.validate("content", tag)
    t.size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=brightcove") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.Brightcove.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Brightcove} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if brightcove embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Brightcove.toString,
        TagAttributes.DataCaption -> "here is a video",
        TagAttributes.DataVideoId -> "1234",
        TagAttributes.DataAccount -> "2183716",
        TagAttributes.DataPlayer -> "B28fas"
      ))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=content-link") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.ContentLink.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ContentLink} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if content-link embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.ContentLink.toString,
        TagAttributes.DataContentId -> "54",
        TagAttributes.DataLinkText -> "interesting article",
        TagAttributes.DataOpenIn -> "new-context"
      ))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=error") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.Error.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Error} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if error embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(TagAttributes.DataResource -> ResourceType.Error.toString,
          TagAttributes.DataMessage -> "interesting article"))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=external") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.ExternalContent.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ExternalContent} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if external embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(TagAttributes.DataResource -> ResourceType.ExternalContent.toString,
          TagAttributes.DataUrl -> "https://www.youtube.com/watch?v=pCZeVTMEsik"))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=nrk") {
    val tag = generateTagWithAttrs(Map(TagAttributes.DataResource -> ResourceType.NRKContent.toString))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.NRKContent} must contain the following attributes:").size should be(
      1)
  }

  test("validate should return no validation errors if nrk embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.NRKContent.toString,
        TagAttributes.DataNRKVideoId -> "123",
        TagAttributes.DataUrl -> "http://nrk.no/video/123"
      ))
    embedTagValidator.validate("content", tag).size should be(0)
  }

  test("validate should fail if only one optional attribute is specified") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataAlt -> "123",
        TagAttributes.DataCaption -> "123",
        TagAttributes.DataResource_Id -> "123",
        TagAttributes.DataSize -> "full",
        TagAttributes.DataAlign -> "left",
        TagAttributes.DataUpperLeftX -> "0",
        TagAttributes.DataFocalX -> "0"
      ))
    embedTagValidator.validate("content", tag).size should be(2)
  }

  test("validate should succeed if all optional attributes are specified") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataAlt -> "123",
        TagAttributes.DataCaption -> "123",
        TagAttributes.DataResource_Id -> "123",
        TagAttributes.DataSize -> "full",
        TagAttributes.DataAlign -> "left",
        TagAttributes.DataUpperLeftX -> "0",
        TagAttributes.DataUpperLeftY -> "0",
        TagAttributes.DataLowerRightX -> "1",
        TagAttributes.DataLowerRightY -> "1",
        TagAttributes.DataFocalX -> "0",
        TagAttributes.DataFocalY -> "1"
      ))

    embedTagValidator.validate("content", tag).size should be(0)
  }

  test("validate should succeed if all attributes in an attribute group are specified") {
    val tagWithFocal = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataAlt -> "123",
        TagAttributes.DataCaption -> "123",
        TagAttributes.DataResource_Id -> "123",
        TagAttributes.DataSize -> "full",
        TagAttributes.DataAlign -> "left",
        TagAttributes.DataFocalX -> "0",
        TagAttributes.DataFocalY -> "1"
      ))

    embedTagValidator.validate("content", tagWithFocal).size should be(0)

    val tagWithCrop = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.Image.toString,
        TagAttributes.DataAlt -> "123",
        TagAttributes.DataCaption -> "123",
        TagAttributes.DataResource_Id -> "123",
        TagAttributes.DataSize -> "full",
        TagAttributes.DataAlign -> "left",
        TagAttributes.DataUpperLeftX -> "0",
        TagAttributes.DataUpperLeftY -> "0",
        TagAttributes.DataLowerRightX -> "1",
        TagAttributes.DataLowerRightY -> "1"
      ))

    embedTagValidator.validate("content", tagWithCrop).size should be(0)
  }

  test("validate should succeed if source url is from a legal domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.IframeContent.toString,
        TagAttributes.DataUrl -> "https://prezi.com",
        TagAttributes.DataWidth -> "1",
        TagAttributes.DataHeight -> "1"
      )
    )
    embedTagValidator.validate("content", tag).size should be(0)

    val tag2 = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.IframeContent.toString,
        TagAttributes.DataUrl -> "https://statisk.test.ndla.no",
        TagAttributes.DataWidth -> "1",
        TagAttributes.DataHeight -> "1"
      )
    )
    embedTagValidator.validate("content", tag2).size should be(0)
  }

  test("validate should fail if source url is from an illlegal domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.IframeContent.toString,
        TagAttributes.DataUrl -> "https://evilprezi.com",
        TagAttributes.DataWidth -> "1",
        TagAttributes.DataHeight -> "1"
      ))

    val result = embedTagValidator.validate("content", tag)
    result.size should be(1)
    result.head.message
      .contains(s"can only contain ${TagAttributes.DataUrl} urls from the following domains:") should be(true)
  }

  test("validate should succeed if source url is from a legal wildcard domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttributes.DataResource -> ResourceType.IframeContent.toString,
        TagAttributes.DataUrl -> "https://thisisatest.khanacademy.org",
        TagAttributes.DataWidth -> "1",
        TagAttributes.DataHeight -> "1"
      ))

    embedTagValidator.validate("content", tag).size should be(0)
  }

  test("validate should not return validation errors if lang attribute is present") {
    val res = embedTagValidator.validate("content", "<span lang='nb'>test</span>")
    res.size should be(0)
  }

  test("validate should not return validation errors if lang attribute is present dd") {
    val res = embedTagValidator.validate("content", "<span test='nb'>test</span>")
    res.size should equal(2)
    findErrorByMessage(res, "An HTML tag 'span' contains an illegal attribute(s) 'test'. Allowed attributes are lang").size should be(
      1)
  }

  test("validate should return not return validation errors if colspan is present on th or td") {
    val res = embedTagValidator.validate(
      "content",
      "<table><thead><tr><th colspan=\"2\">Hei</th></tr></thead><tbody><tr><td>Hei</td><td>Hå</td></tr></tbody></table>")
    res.size should equal(0)
  }

  test("validate should return error if span does not have any attributes") {
    val res = embedTagValidator.validate("content", "<span>lorem ipsum</span>")
    res.size should equal(1)
  }

  test("validate should return error if related content doesnt contain either ids or url and title") {
    val validRelatedExternalEmbed =
      """<embed data-resource="related-content" data-url="http://example.com" data-title="Eksempel tittel right here, yo">"""
    val validRelatedArticle = """<embed data-resource="related-content" data-article-id="5">"""
    val invalidRelatedArticle = """<embed data-resource="related-content" data-url="http://example.com">"""
    val emptyAndInvalidEmbed = """<embed data-resource="related-content">"""

    val res = embedTagValidator.validate(
      "content",
      s"""<div data-type="related-content">$validRelatedExternalEmbed$validRelatedArticle</div>""")
    res.size should be(0)
    val res2 =
      embedTagValidator.validate("content", s"""<div data-type="related-content">$invalidRelatedArticle</div>""")
    res2.size should be(1)
    val res3 =
      embedTagValidator.validate("content", s"""<div data-type="related-content">$emptyAndInvalidEmbed</div>""")
    res3.size should be(1)
  }

  test("validate should return error if related content is not wrapped in div with data-type='related-content'") {
    val validRelatedExternalEmbed =
      """<embed data-resource="related-content" data-url="http://example.com" data-title="Eksempel tittel right here, yo">"""

    val res = embedTagValidator.validate("content", s"""<div>$validRelatedExternalEmbed</div>""")
    res.size should be(1)
    res.head.message should be(
      """Embed tag with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'""")
    val res2 = embedTagValidator.validate("content", s"""$validRelatedExternalEmbed""")
    res2.size should be(1)
    res2.head.message should be(
      """Embed tag with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'""")
    val res3 =
      embedTagValidator.validate("content", s"""<p data-type='related-content'>$validRelatedExternalEmbed</p>""")
    res3.size should be(2)
    res3.last.message should be(
      """Embed tag with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'""")
  }

  test("validate should not return error if parent does not exists and validateParent is false") {
    val validRelatedExternalEmbed =
      """<embed data-resource="related-content" data-url="http://example.com" data-title="Eksempel tittel right here, yo">"""

    val res =
      embedTagValidator.validate("content", s"""<div>$validRelatedExternalEmbed</div>""", validateParent = false)
    res.size should be(0)
  }

  test("checkParentConditions should work for < operator") {
    val result1 = embedTagValidator.checkParentConditions("test", Condition("apekatt<2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = embedTagValidator.checkParentConditions("test", Condition("<2"), 3)
    result2 should be(Right(false))

    val result3 = embedTagValidator.checkParentConditions("test", Condition("<2"), 1)
    result3 should be(Right(true))

    val result4 = embedTagValidator.checkParentConditions("test", Condition("< 2"), 1)
    result4 should be(Right(true))
  }

  test("checkParentConditions should work for > operator") {
    val result1 = embedTagValidator.checkParentConditions("test", Condition("apekatt>2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = embedTagValidator.checkParentConditions("test", Condition(">2"), 3)
    result2 should be(Right(true))

    val result3 = embedTagValidator.checkParentConditions("test", Condition(">2"), 1)
    result3 should be(Right(false))

    val result4 = embedTagValidator.checkParentConditions("test", Condition(">    2"), 1)
    result4 should be(Right(false))
  }

  test("checkParentConditions should work for = operator") {
    val result1 = embedTagValidator.checkParentConditions("test", Condition("apekatt=2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = embedTagValidator.checkParentConditions("test", Condition("=2"), 2)
    result2 should be(Right(true))

    val result3 = embedTagValidator.checkParentConditions("test", Condition("=2"), 1)
    result3 should be(Right(false))

    val result4 = embedTagValidator.checkParentConditions("test", Condition(" =  2 "), 2)
    result4 should be(Right(true))

    val result5 = embedTagValidator.checkParentConditions("test", Condition("2"), 1)
    result5 should be(Left(Seq(ValidationMessage("test", "Could not find supported operator (<, > or =)"))))
  }

  test("validate should should no longer allow single file embeds with multiple unrelated siblings") {
    val content =
      """<section><embed data-alt="Øvingsark for teiknskriving for leksjon 1" data-path="files/147739/ovelsesark_for_tegnskriving_for_leksjon_1.pdf" data-resource="file" data-title="Øvelsesark for tegnskriving for leksjon 1" data-type="pdf"><p><span data-size="large">你</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163943"><p><span data-size="large">您</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163944"><p><span data-size="large">好</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163946"><p><span data-size="large">李</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163948"><p><span data-size="large">美</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163950"><p><span data-size="large">玉</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163952"><p><span data-size="large">马</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163953"><p><span data-size="large">红</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163956"><p><span data-size="large">老</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163959"><p><span data-size="large">师</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163960"><p><span data-size="large">贵</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164025"><p><span data-size="large">姓</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164029"><p><span data-size="large">王</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164031"><p><span data-size="large">们</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164032"><p><span data-size="large">我</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164034"><p><span data-size="large">叫</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164035"><p><span data-size="large">什</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164036"><p><span data-size="large">么</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164037"><p><span data-size="large">名</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164038"><p><span data-size="large">字</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164039"><p><span data-size="large">呢</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164067"><p><span data-size="large">认</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164069"><p><span data-size="large">识</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164072"><p><span data-size="large">很</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164077"><p><span data-size="large">高</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164078"><p><span data-size="large">兴</span></p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164079"></section>"""

    val res = embedTagValidator.validate("content", content)
    res should be(
      Seq(ValidationMessage("content",
                            "Embed tag with 'file' requires a parent 'div', with attributes: 'data-type=\"file\"'")))
  }

  test("That getNumEqualSiblings returns number of direct equal siblings") {
    {
      val content = """<section><p><embed type="a" data-resource="file"></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select("embed").first()
      embedTagValidator.numDirectEqualSiblings(embed) should be(1)
    }
    {
      val content =
        """<section><p><embed type="a" data-resource="file"><embed type="b" data-resource="file"><embed type="c" data-resource="file"><embed type="d" data-resource="file"></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select("embed[type=b]").first()
      embedTagValidator.numDirectEqualSiblings(embed) should be(4)
    }
    {
      val content =
        """<section><p><embed type="a" data-resource="file">, <embed type="b" data-resource="file">, <embed type="c" data-resource="file">, <embed type="d" data-resource="file"></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select("embed[type=d]").first()
      embedTagValidator.numDirectEqualSiblings(embed) should be(1)
    }
    {
      val content =
        """<section><p><embed type="a" data-resource="file">, <embed type="b" data-resource="file">, <embed type="c" data-resource="file"><embed type="d" data-resource="file"></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select("embed[type=c]").first()
      embedTagValidator.numDirectEqualSiblings(embed) should be(2)
    }
  }

  test("getNumEqualSiblings should ignore only-whitespace siblings, but not text siblings") {
    val content =
      """<section>
          |<p>
          |<embed type="a" data-resource="file">awdk
          |<embed type="b" data-resource="file">
          |
          |<embed type="c" data-resource="file">
          |
          |
          |
          |
          |
          |<embed type="d" data-resource="file">
          |</p>
          |</section>""".stripMargin
    val embed = HtmlTagRules.stringToJsoupDocument(content).select("embed[type=c]").first()
    embedTagValidator.numDirectEqualSiblings(embed) should be(3)
  }

}
