/*
 * Part of NDLA validation
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationMessage
import no.ndla.validation.TagRules.Condition

class EmbedTagValidatorTest extends UnitSuite {

  private def childCountValidationMessage(fieldName: String): Seq[ValidationMessage] =
    Seq(
      ValidationMessage(
        fieldName,
        "Parent condition block is invalid. " +
          "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1')."
      )
    )

  private def generateAttributes(attrs: Map[String, String]): String = {
    attrs.toList.sortBy(_._1).map { case (key, value) => s"""$key="$value"""" }.mkString(" ")
  }

  private def generateTagWithAttrs(attrs: Map[TagAttribute, String]): String = {
    val strAttrs = attrs map { case (k, v) => k.toString -> v }
    s"""<$EmbedTagName ${generateAttributes(strAttrs)}></$EmbedTagName>"""
  }

  private def generateTagWithAttrsAndChildren(attrs: Map[TagAttribute, String], children: String): String = {
    val strAttrs = attrs map { case (k, v) => k.toString -> v }
    s"""<$EmbedTagName ${generateAttributes(strAttrs)}>$children</$EmbedTagName>"""
  }

  private def findErrorByMessage(
      validationMessages: Seq[ValidationMessage],
      partialMessage: String
  ): Option[ValidationMessage] =
    validationMessages.find(_.message.contains(partialMessage))

  test("validate should return an empty sequence if input is not an embed tag or an html tag with attributes") {
    TagValidator.validate("content", "<h1>hello</h1>") should equal(Seq())
  }

  test("validate should return a validation error if input is a html tag with that should not have attributes") {
    val res = TagValidator.validate("content", "<h1 test='test'>hello</h1>")
    findErrorByMessage(res, "Tag 'h1' contains an illegal attribute(s) 'test'.").size should be(1)
  }

  test("validate should return validation error if embed tag uses illegal attributes") {
    val attrs = generateAttributes(
      Map(
        TagAttribute.DataResource.toString -> ResourceType.ExternalContent.toString,
        TagAttribute.DataUrl.toString      -> "google.com",
        "illegalattr"                      -> "test"
      )
    )

    val res = TagValidator.validate("content", s"""<$EmbedTagName $attrs />""")
    findErrorByMessage(res, "illegal attribute(s) 'illegalattr'").size should be(1)
  }

  test("validate should return validation error if an attribute contains HTML") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.ExternalContent.toString,
        TagAttribute.DataUrl      -> "<i>google.com</i>"
      )
    )
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(res, "contains attributes with HTML: data-url").size should be(1)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=image"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.Image.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.Image} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return not validation error if embed tag misses moved required to optional") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource    -> ResourceType.Image.toString,
        TagAttribute.DataResource_Id -> "1234",
        TagAttribute.DataSize        -> "full",
        TagAttribute.DataAlign       -> "",
        TagAttribute.DataAlt         -> "alttext"
      )
    )
    val res = TagValidator.validate(
      "content",
      tag,
      requiredToOptional = Map("image" -> Seq(TagAttribute.DataCaption.toString))
    )
    res should be(Seq.empty)
  }

  test("validate should return no validation errors if image embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource    -> ResourceType.Image.toString,
        TagAttribute.DataResource_Id -> "1234",
        TagAttribute.DataSize        -> "full",
        TagAttribute.DataAlt         -> "alternative text",
        TagAttribute.DataCaption     -> "here is a rabbit",
        TagAttribute.DataAlign       -> "left"
      )
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=audio"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.Audio.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.Audio} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if audio embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource    -> ResourceType.Audio.toString,
        TagAttribute.DataResource_Id -> "1234",
        TagAttribute.DataCaption     -> "",
        TagAttribute.DataType        -> "standard"
      )
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=h5p"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.H5P.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.H5P} must contain the following attributes:").size should be(
      1
    )
  }

  test("validate should return no validation errors if h5p embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.H5P.toString,
        TagAttribute.DataPath     -> "/h5p/embed/1234"
      )
    )
    val t = TagValidator.validate("content", tag)
    t.size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=brightcove"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.Brightcove.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.Brightcove} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if brightcove embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.Brightcove.toString,
        TagAttribute.DataCaption  -> "here is a video",
        TagAttribute.DataVideoId  -> "1234",
        TagAttribute.DataAccount  -> "2183716",
        TagAttribute.DataPlayer   -> "B28fas"
      )
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=content-link"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.ContentLink.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.ContentLink} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if content-link embed-tag is used correctly") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource  -> ResourceType.ContentLink.toString,
        TagAttribute.DataContentId -> "54",
        TagAttribute.DataOpenIn    -> "new-context"
      ),
      "interesting article"
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test("validate should return validation errors if content-link embed-tag is used incorrectly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource  -> ResourceType.ContentLink.toString,
        TagAttribute.DataContentId -> "54",
        TagAttribute.DataOpenIn    -> "new-context"
      )
    )
    TagValidator.validate("content", tag).size should be(1)
  }

  test("validate should return allow valid children for content-link") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource  -> ResourceType.ContentLink.toString,
        TagAttribute.DataContentId -> "54",
        TagAttribute.DataOpenIn    -> "new-context"
      ),
      "<strong>Apekatt</strong> er kult"
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test("validate should return validation errors if content-link has invalid children") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource  -> ResourceType.ContentLink.toString,
        TagAttribute.DataContentId -> "54",
        TagAttribute.DataOpenIn    -> "new-context"
      ),
      "<div><strong>Apekatt</strong> er kult</div>"
    )
    TagValidator.validate("content", tag).size should be(1)
  }

  test("validate should return no validation errors if uu-disclaimer embed-tag is used correctly with embed") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource   -> ResourceType.UuDisclaimer.toString,
        TagAttribute.DataDisclaimer -> "Dette er en disclaimer"
      ),
      generateTagWithAttrs(
        Map(
          TagAttribute.DataResource    -> ResourceType.Image.toString,
          TagAttribute.DataResource_Id -> "1234",
          TagAttribute.DataSize        -> "full",
          TagAttribute.DataAlt         -> "alternative text",
          TagAttribute.DataCaption     -> "here is a rabbit",
          TagAttribute.DataAlign       -> "left"
        )
      )
    )
    val res = TagValidator.validate("content", tag)
    res.size should be(0)
  }

  test("validate should return no validation errors if uu-disclaimer embed-tag is used correctly with html") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource   -> ResourceType.UuDisclaimer.toString,
        TagAttribute.DataDisclaimer -> "Dette er en disclaimer"
      ),
      """
        |<p>Her er en disclaimer</p>
        |<details>
        |<summary>Tittel</summary>
        |Innhold
        |</details>
        |<dl>
        |<dt>Term</dt>
        |<dd>Definisjon</dd>
        |</dl>
        |<div data-type="framed-content"><p>Tekst</p></div>
        |""".stripMargin
    )
    val res = TagValidator.validate("content", tag)
    res.size should be(0)
  }

  test("validate should return no validation errors if comment embed-tag is used correctly with html") {
    val tag = generateTagWithAttrsAndChildren(
      Map(
        TagAttribute.DataResource -> ResourceType.Comment.toString,
        TagAttribute.DataText     -> "Min kommentar",
        TagAttribute.DataType     -> "inline"
      ),
      """
        |<p>her</p>
        |""".stripMargin
    )
    val res = TagValidator.validate("content", tag)
    res.size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=error"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.Error.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.Error} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if error embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(TagAttribute.DataResource -> ResourceType.Error.toString, TagAttribute.DataMessage -> "interesting article")
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=external"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.ExternalContent.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.ExternalContent} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if external embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.ExternalContent.toString,
        TagAttribute.DataUrl      -> "https://www.youtube.com/watch?v=pCZeVTMEsik",
        TagAttribute.DataType     -> "external"
      )
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test(
    "validate should return validation error if embed tag does not contain required attributes for data-resource=nrk"
  ) {
    val tag = generateTagWithAttrs(Map(TagAttribute.DataResource -> ResourceType.NRKContent.toString))
    val res = TagValidator.validate("content", tag)
    findErrorByMessage(
      res,
      s"data-resource=${ResourceType.NRKContent} must contain the following attributes:"
    ).size should be(1)
  }

  test("validate should return no validation errors if nrk embed-tag is used correctly") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource   -> ResourceType.NRKContent.toString,
        TagAttribute.DataNRKVideoId -> "123",
        TagAttribute.DataUrl        -> "http://nrk.no/video/123"
      )
    )
    TagValidator.validate("content", tag).size should be(0)
  }

  test("validate should fail if only one optional attribute is specified") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource    -> ResourceType.Image.toString,
        TagAttribute.DataAlt         -> "123",
        TagAttribute.DataCaption     -> "123",
        TagAttribute.DataResource_Id -> "123",
        TagAttribute.DataSize        -> "full",
        TagAttribute.DataAlign       -> "left",
        TagAttribute.DataUpperLeftX  -> "0",
        TagAttribute.DataFocalX      -> "0"
      )
    )
    val messages = TagValidator.validate("content", tag)
    messages.size should be(2)
  }

  test("validate should succeed if all optional attributes are specified") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource     -> ResourceType.Image.toString,
        TagAttribute.DataAlt          -> "123",
        TagAttribute.DataCaption      -> "123",
        TagAttribute.DataResource_Id  -> "123",
        TagAttribute.DataSize         -> "full",
        TagAttribute.DataAlign        -> "left",
        TagAttribute.DataUpperLeftX   -> "0",
        TagAttribute.DataUpperLeftY   -> "0",
        TagAttribute.DataLowerRightX  -> "1",
        TagAttribute.DataLowerRightY  -> "1",
        TagAttribute.DataFocalX       -> "0",
        TagAttribute.DataFocalY       -> "1",
        TagAttribute.DataIsDecorative -> "false"
      )
    )

    TagValidator.validate("content", tag).size should be(0)
  }

  test("validate should succeed if all attributes in an attribute group are specified") {
    val tagWithFocal = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource    -> ResourceType.Image.toString,
        TagAttribute.DataAlt         -> "123",
        TagAttribute.DataCaption     -> "123",
        TagAttribute.DataResource_Id -> "123",
        TagAttribute.DataSize        -> "full",
        TagAttribute.DataAlign       -> "left",
        TagAttribute.DataFocalX      -> "0",
        TagAttribute.DataFocalY      -> "1"
      )
    )

    TagValidator.validate("content", tagWithFocal).size should be(0)

    val tagWithCrop = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource     -> ResourceType.Image.toString,
        TagAttribute.DataAlt          -> "123",
        TagAttribute.DataCaption      -> "123",
        TagAttribute.DataResource_Id  -> "123",
        TagAttribute.DataSize         -> "full",
        TagAttribute.DataAlign        -> "left",
        TagAttribute.DataUpperLeftX   -> "0",
        TagAttribute.DataUpperLeftY   -> "0",
        TagAttribute.DataLowerRightX  -> "1",
        TagAttribute.DataLowerRightY  -> "1",
        TagAttribute.DataIsDecorative -> "false"
      )
    )

    TagValidator.validate("content", tagWithCrop).size should be(0)

    val tagWithDecor = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource     -> ResourceType.Image.toString,
        TagAttribute.DataAlt          -> "123",
        TagAttribute.DataCaption      -> "123",
        TagAttribute.DataResource_Id  -> "123",
        TagAttribute.DataSize         -> "full",
        TagAttribute.DataAlign        -> "left",
        TagAttribute.DataIsDecorative -> "false"
      )
    )

    TagValidator.validate("content", tagWithDecor).size should be(0)
  }

  test("validate should succeed if source url is from a legal domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.IframeContent.toString,
        TagAttribute.DataType     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl      -> "https://prezi.com",
        TagAttribute.DataWidth    -> "1",
        TagAttribute.DataHeight   -> "1"
      )
    )
    TagValidator.validate("content", tag).size should be(0)

    val tag2 = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.IframeContent.toString,
        TagAttribute.DataType     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl      -> "https://statisk.test.ndla.no",
        TagAttribute.DataWidth    -> "1",
        TagAttribute.DataHeight   -> "1"
      )
    )
    TagValidator.validate("content", tag2).size should be(0)
  }

  test("validate should succeed if source url is not required and empty") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource     -> ResourceType.CampaignBlock.toString,
        TagAttribute.DataTitle        -> "Title",
        TagAttribute.DataDescription  -> "Description",
        TagAttribute.DataHeadingLevel -> "h3"
      )
    )
    TagValidator.validate("content", tag).size should be(0)

    val tag2 = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource     -> ResourceType.CampaignBlock.toString,
        TagAttribute.DataTitle        -> "Title",
        TagAttribute.DataDescription  -> "Description",
        TagAttribute.DataHeadingLevel -> "h3",
        TagAttribute.DataUrl          -> "",
        TagAttribute.DataUrlText      -> ""
      )
    )
    TagValidator.validate("content", tag2).size should be(0)

  }

  test("validate should fail if source url is from an illlegal domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.IframeContent.toString,
        TagAttribute.DataType     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl      -> "https://evilprezi.com",
        TagAttribute.DataWidth    -> "1",
        TagAttribute.DataHeight   -> "1"
      )
    )

    val result = TagValidator.validate("content", tag)
    result.size should be(1)
    result.head.message
      .contains(s"can only contain urls from the following domains:") should be(true)
  }

  test("validate should fail if source url is not a legal address") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.IframeContent.toString,
        TagAttribute.DataType     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl      -> "https://notalegaladdress",
        TagAttribute.DataWidth    -> "1",
        TagAttribute.DataHeight   -> "1"
      )
    )

    val result = TagValidator.validate("content", tag)
    result.size should be(1)
    result.head.message
      .contains(s"must be a valid url address") should be(true)
  }

  test("validate should succeed if source url matches regex") {
    {
      val tag = generateTagWithAttrs(
        Map(
          TagAttribute.DataResource -> ResourceType.Pitch.toString,
          TagAttribute.DataUrl      -> "http://vg.no",
          TagAttribute.DataImage_Id -> "1",
          TagAttribute.DataTitle    -> "Blogpost"
        )
      )

      val result = TagValidator.validate("content", tag)
      result.size should be(0)
    }
    {
      val tag = generateTagWithAttrs(
        Map(
          TagAttribute.DataResource -> ResourceType.Pitch.toString,
          TagAttribute.DataUrl      -> "http://vg.no?this=is&also=valid",
          TagAttribute.DataImage_Id -> "1",
          TagAttribute.DataTitle    -> "Blogpost"
        )
      )

      val result = TagValidator.validate("content", tag)
      result.size should be(0)
    }
    {
      val tag = generateTagWithAttrs(
        Map(
          TagAttribute.DataResource -> ResourceType.Pitch.toString,
          TagAttribute.DataUrl      -> "https://vg.no?t=",
          TagAttribute.DataImage_Id -> "1",
          TagAttribute.DataTitle    -> "Blogpost"
        )
      )

      val result = TagValidator.validate("content", tag)
      result.size should be(0)
    }

  }

  test("validate should succeed if source url is from a legal wildcard domain") {
    val tag = generateTagWithAttrs(
      Map(
        TagAttribute.DataResource -> ResourceType.IframeContent.toString,
        TagAttribute.DataType     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl      -> "https://thisisatest.khanacademy.org",
        TagAttribute.DataWidth    -> "1",
        TagAttribute.DataHeight   -> "1"
      )
    )

    TagValidator.validate("content", tag).size should be(0)
  }

  test("validate should not return validation errors if lang attribute is present") {
    val res = TagValidator.validate("content", "<span lang='nb'>test</span>")
    res.size should be(0)
  }

  test("validate should not return validation errors if lang attribute is present dd") {
    val res = TagValidator.validate("content", "<span test='nb'>test</span>")
    res.size should equal(2)
    findErrorByMessage(
      res,
      "Tag 'span' contains an illegal attribute(s) 'test'. Allowed attributes are lang"
    ).size should be(1)
  }

  test("validate should return not return validation errors if colspan is present on th or td") {
    val res = TagValidator.validate(
      "content",
      "<table><thead><tr><th colspan=\"2\">Hei</th></tr></thead><tbody><tr><td>Hei</td><td>Hå</td></tr></tbody></table>"
    )
    res.size should equal(0)
  }

  test("validate should return error if span does not have any attributes") {
    val res = TagValidator.validate("content", "<span>lorem ipsum</span>")
    res.size should equal(1)
  }

  test("validate should return error if related content doesnt contain either ids or url and title") {
    val validRelatedExternalEmbed =
      s"""<$EmbedTagName data-resource="related-content" data-url="http://example.com" data-title="Eksempel tittel right here, yo"></$EmbedTagName>"""
    val validRelatedArticle = s"""<$EmbedTagName data-resource="related-content" data-article-id="5"></$EmbedTagName>"""
    val invalidRelatedArticle =
      s"""<$EmbedTagName data-resource="related-content" data-url="http://example.com"></$EmbedTagName>"""
    val emptyAndInvalidEmbed = s"""<$EmbedTagName data-resource="related-content"></$EmbedTagName>"""

    val res = TagValidator.validate(
      "content",
      s"""<div data-type="related-content">$validRelatedExternalEmbed$validRelatedArticle</div>"""
    )
    res.size should be(0)
    val res2 =
      TagValidator.validate("content", s"""<div data-type="related-content">$invalidRelatedArticle</div>""")
    res2.size should be(1)
    val res3 =
      TagValidator.validate("content", s"""<div data-type="related-content">$emptyAndInvalidEmbed</div>""")
    res3.size should be(1)
  }

  test("validate should return error if related content is not wrapped in div with data-type='related-content'") {
    val validRelatedExternalEmbed =
      s"""<$EmbedTagName data-resource="related-content" data-url="http://example.com" data-title="Eksempel tittel right here, yo"></$EmbedTagName>"""

    val res = TagValidator.validate("content", s"""<div>$validRelatedExternalEmbed</div>""")
    res.size should be(1)
    res.head.message should be(
      """Tag 'ndlaembed' with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'"""
    )
    val res2 = TagValidator.validate("content", s"""$validRelatedExternalEmbed""")
    res2.size should be(1)
    res2.head.message should be(
      """Tag 'ndlaembed' with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'"""
    )
    val res3 =
      TagValidator.validate("content", s"""<p data-type='related-content'>$validRelatedExternalEmbed</p>""")
    res3.size should be(2)
    res3.last.message should be(
      """Tag 'ndlaembed' with 'related-content' requires a parent 'div', with attributes: 'data-type="related-content"'"""
    )
  }

  test("checkParentConditions should work for < operator") {
    val result1 = TagValidator.checkParentConditions("test", Condition("apekatt<2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = TagValidator.checkParentConditions("test", Condition("<2"), 3)
    result2 should be(Right(false))

    val result3 = TagValidator.checkParentConditions("test", Condition("<2"), 1)
    result3 should be(Right(true))

    val result4 = TagValidator.checkParentConditions("test", Condition("< 2"), 1)
    result4 should be(Right(true))
  }

  test("checkParentConditions should work for > operator") {
    val result1 = TagValidator.checkParentConditions("test", Condition("apekatt>2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = TagValidator.checkParentConditions("test", Condition(">2"), 3)
    result2 should be(Right(true))

    val result3 = TagValidator.checkParentConditions("test", Condition(">2"), 1)
    result3 should be(Right(false))

    val result4 = TagValidator.checkParentConditions("test", Condition(">    2"), 1)
    result4 should be(Right(false))
  }

  test("checkParentConditions should work for = operator") {
    val result1 = TagValidator.checkParentConditions("test", Condition("apekatt=2"), 3)
    result1 should be(Left(childCountValidationMessage("test")))

    val result2 = TagValidator.checkParentConditions("test", Condition("=2"), 2)
    result2 should be(Right(true))

    val result3 = TagValidator.checkParentConditions("test", Condition("=2"), 1)
    result3 should be(Right(false))

    val result4 = TagValidator.checkParentConditions("test", Condition(" =  2 "), 2)
    result4 should be(Right(true))

    val result5 = TagValidator.checkParentConditions("test", Condition("2"), 1)
    result5 should be(Left(Seq(ValidationMessage("test", "Could not find supported operator (<, > or =)"))))
  }

  test("validate should should no longer allow single file embeds with multiple unrelated siblings") {
    val content =
      s"""<section><$EmbedTagName data-alt="Øvingsark for teiknskriving for leksjon 1" data-path="files/147739/ovelsesark_for_tegnskriving_for_leksjon_1.pdf" data-resource="file" data-title="Øvelsesark for tegnskriving for leksjon 1" data-type="pdf"></$EmbedTagName>
         |<p><span data-size="large">你</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163943"></$EmbedTagName>
         |<p><span data-size="large">您</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163944"></$EmbedTagName>
         |<p><span data-size="large">好</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163946"></$EmbedTagName>
         |<p><span data-size="large">李</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163948"></$EmbedTagName>
         |<p><span data-size="large">美</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163950"></$EmbedTagName>
         |<p><span data-size="large">玉</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163952"></$EmbedTagName>
         |<p><span data-size="large">马</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163953"></$EmbedTagName>
         |<p><span data-size="large">红</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163956"></$EmbedTagName>
         |<p><span data-size="large">老</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163959"></$EmbedTagName>
         |<p><span data-size="large">师</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:163960"></$EmbedTagName>
         |<p><span data-size="large">贵</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164025"></$EmbedTagName>
         |<p><span data-size="large">姓</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164029"></$EmbedTagName>
         |<p><span data-size="large">王</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164031"></$EmbedTagName>
         |<p><span data-size="large">们</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164032"></$EmbedTagName>
         |<p><span data-size="large">我</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164034"></$EmbedTagName>
         |<p><span data-size="large">叫</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164035"></$EmbedTagName>
         |<p><span data-size="large">什</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164036"></$EmbedTagName>
         |<p><span data-size="large">么</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164037"></$EmbedTagName>
         |<p><span data-size="large">名</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164038"></$EmbedTagName>
         |<p><span data-size="large">字</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164039"></$EmbedTagName>
         |<p><span data-size="large">呢</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164067"></$EmbedTagName>
         |<p><span data-size="large">认</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164069"></$EmbedTagName>
         |<p><span data-size="large">识</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164072"></$EmbedTagName>
         |<p><span data-size="large">很</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164077"></$EmbedTagName>
         |<p><span data-size="large">高</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164078"></$EmbedTagName>
         |<p><span data-size="large">兴</span></p><$EmbedTagName data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:164079"></$EmbedTagName></section>""".stripMargin

    val res = TagValidator.validate("content", content)
    res should be(
      Seq(
        ValidationMessage(
          "content",
          "Tag 'ndlaembed' with 'file' requires a parent 'div', with attributes: 'data-type=\"file\"'"
        )
      )
    )
  }

  test("That getNumEqualSiblings returns number of direct equal siblings") {
    {
      val content = s"""<section><p><$EmbedTagName type="a" data-resource="file" /></p></section>"""
      val embed   = HtmlTagRules.stringToJsoupDocument(content).select(EmbedTagName).first()
      TagValidator.numDirectEqualSiblings(embed) should be(1)
    }
    {
      val content =
        s"""<section><p><$EmbedTagName type="a" data-resource="file"></$EmbedTagName><$EmbedTagName type="b" data-resource="file"></$EmbedTagName><$EmbedTagName type="c" data-resource="file"></$EmbedTagName><$EmbedTagName type="d" data-resource="file"></$EmbedTagName></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select(s"$EmbedTagName[type=b]").first()
      TagValidator.numDirectEqualSiblings(embed) should be(4)
    }
    {
      val content =
        s"""<section><p><$EmbedTagName type="a" data-resource="file"></$EmbedTagName>, <$EmbedTagName type="b" data-resource="file"></$EmbedTagName>, <$EmbedTagName type="c" data-resource="file"></$EmbedTagName>, <$EmbedTagName type="d" data-resource="file"></$EmbedTagName></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select(s"$EmbedTagName[type=d]").first()
      TagValidator.numDirectEqualSiblings(embed) should be(1)
    }
    {
      val content =
        s"""<section><p><$EmbedTagName type="a" data-resource="file"></$EmbedTagName>, <$EmbedTagName type="b" data-resource="file"></$EmbedTagName>, <$EmbedTagName type="c" data-resource="file"></$EmbedTagName><$EmbedTagName type="d" data-resource="file"></$EmbedTagName></p></section>"""
      val embed = HtmlTagRules.stringToJsoupDocument(content).select(s"$EmbedTagName[type=c]").first()
      TagValidator.numDirectEqualSiblings(embed) should be(2)
    }
  }

  test("getNumEqualSiblings should ignore only-whitespace siblings, but not text siblings") {
    val content =
      s"""<section>
          |<p>
          |<$EmbedTagName type="a" data-resource="file"></$EmbedTagName>awdk
          |<$EmbedTagName type="b" data-resource="file"></$EmbedTagName>
          |
          |<$EmbedTagName type="c" data-resource="file"></$EmbedTagName>
          |
          |
          |
          |
          |
          |<$EmbedTagName type="d" data-resource="file"></$EmbedTagName>
          |</p>
          |</section>""".stripMargin
    val embed = HtmlTagRules.stringToJsoupDocument(content).select(s"$EmbedTagName[type=c]").first()
    TagValidator.numDirectEqualSiblings(embed) should be(3)
  }

  test("validate should return no errors when data-title is used as a standalone tag in iframe") {
    val attrs = generateAttributes(
      Map(
        // Iframe Required
        TagAttribute.DataResource.toString -> ResourceType.IframeContent.toString,
        TagAttribute.DataType.toString     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl.toString      -> "https://google.ndla.no/",
        // Iframe Optional-1
        TagAttribute.DataWidth.toString  -> "700",
        TagAttribute.DataHeight.toString -> "500",
        // Iframe Optional that i want to test
        TagAttribute.DataTitle.toString -> "Min tittel"
      )
    )

    val res = TagValidator.validate("content", s"""<$EmbedTagName $attrs></$EmbedTagName>""")
    res.headOption should be(None)
  }

  test("validate should return no errors when data-title is used in a group in iframe") {
    val attrs = generateAttributes(
      Map(
        TagAttribute.DataResource.toString -> ResourceType.IframeContent.toString,
        TagAttribute.DataType.toString     -> ResourceType.IframeContent.toString,
        TagAttribute.DataUrl.toString      -> "https://google.ndla.no/",
        TagAttribute.DataWidth.toString    -> "700",
        TagAttribute.DataHeight.toString   -> "500",
        TagAttribute.DataTitle.toString    -> "Min tittel",
        TagAttribute.DataCaption.toString  -> "heyho",
        TagAttribute.DataImageId.toString  -> "123"
      )
    )

    val res = TagValidator.validate("content", s"""<$EmbedTagName $attrs></$EmbedTagName>""")
    res.headOption should be(None)
  }
}
