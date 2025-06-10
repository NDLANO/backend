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

class EmbedTagRulesTest extends UnitSuite {

  test("Rules for all resource types should be defined") {
    val resourceTypesFromConfigFile      = EmbedTagRules.attributeRules.keys
    val resourceTypesFromEnumDeclaration = ResourceType.values

    resourceTypesFromEnumDeclaration.foreach(rt => {
      resourceTypesFromConfigFile.should(contain(rt))
    })
  }

  test("data-resource should be required for all resource types") {
    val resourceTypesFromConfigFile = EmbedTagRules.attributeRules.keys

    resourceTypesFromConfigFile.foreach(resType =>
      EmbedTagRules.attributesForResourceType(resType).required.map(f => f.name) should contain(
        TagAttribute.DataResource
      )
    )
  }

  test("Every mustBeDirectChildOf -> condition block must be valid") {

    EmbedTagRules.attributeRules.flatMap { case (tag, rule) =>
      rule.mustBeDirectChildOf.flatMap(parentRule => {
        parentRule.conditions.map(c => {
          val res = TagValidator.checkParentConditions(tag.toString, c, 1)
          res.isRight should be(true)
        })
      })
    }

    val result1 = TagValidator.checkParentConditions("test", Condition("apekatt=2"), 3)
    result1 should be(
      Left(
        Seq(
          ValidationMessage(
            "test",
            "Parent condition block is invalid. " +
              "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1')."
          )
        )
      )
    )
  }

  test("Required fields with dataType NUMBER should not be allowed to be empty") {
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="image"
           | data-resource_id=""
           | data-size=""
           | data-align=""
           | data-alt=""
           | data-caption=""
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=image and attribute data-resource_id= must have a valid numeric value."
          )
        )
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="campaign-block"
           | data-title="Marvellous campaign"
           | data-description="Water is good for you!"
           | data-heading-level="h1"
           | data-url="https://blogg.ndla.no/campaign"
           | data-url-text="Our blog"
           | data-image-id=""
           | data-image-side="left"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
  }

  test("Fields with dataType BOOLEAN should in fact be boolean") {
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="key-figure"
           | data-title="test"
           | data-subtitle="test"
           | data-is-decorative="wat"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=key-figure and attribute data-is-decorative=wat must have a valid boolean value."
          )
        )
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="key-figure"
           | data-title="test"
           | data-subtitle="test"
           | data-is-decorative="true"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
  }

  test("Field with dataType URL should accept funky embed urls") {
    {
      val url =
        "https://www.gapminder.org/tools/#$ui$chart$cursorMode=plus&opacitySelectDim:0.39;;&model$markers$bubble$encoding$size$data$space@=geo&=time;;&scale$domain:null&type:null&zoomed:null;;&x$data$concept=time&space@=time;;&scale$domain:null&zoomed:null&type:null;;&frame$extrapolate:51;&trail$data$filter$markers$bra=1800&nor=1800&uga=1800;;;;;;;;&chart-type=bubbles&url=v1"
      val embedString =
        s"""<$EmbedTagName
           | data-resource="iframe"
           | data-url="$url"
           | data-type="iframe"
           | data-title="Gapminder"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val url =
        "https://norgeskart.no/#!?project=norgeskart&layers=1002&zoom=16&lat=6629573.59&lon=-9409.52&markerLat=6629453.716012445&markerLon=-8055.200595151538&p=searchOptionsPanel&sok=Haukadalen"
      val embedString =
        s"""<$EmbedTagName
           | data-resource="iframe"
           | data-url="$url"
           | data-type="iframe"
           | data-title="Norgeskart"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val url =
        "https://kartiskolen.no/mobile.html?topic=geologi&lang=nb&bgLayer=vanlig_grunnkart&mobile=true&layers=bergarter_oversikt,bergarter_detaljer&layers_opacity=0.6,0.6&X=6758065.33&Y=8776.16&zoom=10"
      val embedString =
        s"""<$EmbedTagName
           | data-resource="iframe"
           | data-url="$url"
           | data-type="iframe"
           | data-title="Kart i skolen"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
  }

  test("Fields with dataType EMAIL should have legal email") {
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="contact-block"
           | data-job-title="Batman"
           | data-name="Bruce Wayne"
           | data-email="batman@gotham.com"
           | data-description="The original broody superhero"
           | data-image-id="1"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="contact-block"
           | data-job-title="Batman"
           | data-name="Bruce Wayne"
           | data-email="thisisinfactalegal@email-address"
           | data-description="The original broody superhero"
           | data-image-id="1"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="contact-block"
           | data-job-title="Batman"
           | data-name="Bruce Wayne"
           | data-email=""
           | data-description="The original broody superhero"
           | data-image-id="1"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=contact-block and data-email= must be a valid email address."
          )
        )
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="contact-block"
           | data-job-title="Batman"
           | data-name="Bruce Wayne"
           | data-email="batman_at_gotham_dot_com"
           | data-description="The original broody superhero"
           | data-image-id="1"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=contact-block and data-email=batman_at_gotham_dot_com must be a valid email address."
          )
        )
      )
    }
  }

  test("Fields with dataType LIST should in fact be a list") {
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="concept"
           | data-content-id="1"
           | data-type="gloss"
           | data-example-langs="{nb}"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=concept and attribute data-example-langs={nb} must be a string or list of strings."
          )
        )
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="concept"
           | data-content-id="1"
           | data-type="gloss"
           | data-example-langs="[nb]"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=concept and attribute data-example-langs=[nb] must be a string or list of strings."
          )
        )
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="concept"
           | data-content-id="1"
           | data-type="gloss"
           | data-example-langs="nb-NO"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="concept"
           | data-content-id="1"
           | data-type="gloss"
           | data-example-langs="nb,nn,en-UK"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="concept"
           | data-content-id="1"
           | data-type="gloss"
           | data-example-ids="0,2"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
  }

  test("Fields with dataType JSON should be valid json") {
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="copyright"
           | data-title="Tittel"
           | data-copyright="{&quot;license&quot;:&quot;cc-by&quot;,&quot;origin&quot;:&quot;https://snl.no&quot;}"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq.empty
      )
    }
    {
      val embedString =
        s"""<$EmbedTagName
           | data-resource="copyright"
           | data-title="Tittel"
           | data-copyright="No JSON here"
           |/>""".stripMargin

      val result = TagValidator.validate("test", embedString)
      result should be(
        Seq(
          ValidationMessage(
            "test",
            s"Tag '$EmbedTagName' with data-resource=copyright and attribute data-copyright=No JSON here must be valid json."
          )
        )
      )
    }
  }

  test("Optional standalone fields without coExisting is OK") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="external"
         | data-url="https://youtube.com"
         | data-type="external"
         | data-title="Youtube-video"
         |/>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq.empty
    )
  }

  test("Optional fields dependent on others is !OK") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="external"
         | data-url="https://youtube.com"
         | data-type="external"
         | data-title="Youtube-video"
         | data-imageid="123"
         |/>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"Tag '$EmbedTagName' with data-resource=external must contain all or none of the attributes in the optional attribute group: (data-caption (Missing: data-caption))"
        )
      )
    )
  }

  test("Html in data-alt is forbidden for image") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="image"
         | data-resource_id="1"
         | data-size=""
         | data-align=""
         | data-alt="Bilde på <span lang='en'>engelsk</span>"
         | data-caption="Bilde på <span lang='en'>engelsk</span>"
         |/>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"Tag '$EmbedTagName' contains attributes with HTML: data-alt"
        )
      )
    )
  }

  test("Html in data-title is ok for pitch") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="pitch"
         | data-image-id="1"
         | data-title="Hva skjer hos <span lang='en'>NDLA</span>"
         | data-url="https://ndla.no"
         |/>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq.empty
    )
  }

  test("Children is not ok for embed without children rule") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="pitch"
         | data-image-id="1"
         | data-title="Hva skjer hos <span lang='en'>NDLA</span>"
         | data-url="https://ndla.no">
         | Some child
         | </$EmbedTagName>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq {
        ValidationMessage("test", s"Tag '$EmbedTagName' with `data-resource=pitch` cannot have children.")
      }
    )
  }

  test("Symbol embed with plain text children and no attributes is ok") {
    val embedString = s"""<$EmbedTagName data-resource="symbol">Symbol</$EmbedTagName>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq.empty
    )
  }

  test("Symbol embed with no children is not ok") {
    val embedString =
      s"""<$EmbedTagName data-resource="symbol"></$EmbedTagName>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"Tag '$EmbedTagName' with `data-resource=symbol` requires at least one child."
        )
      )
    )
  }

  test("Symbol embed with html children is not ok") {
    val embedString =
      s"""<$EmbedTagName data-resource="symbol"><strong>Symbol</strong></$EmbedTagName>""".stripMargin

    val result = TagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"Tag '$EmbedTagName' with `data-resource=symbol` can only have plaintext children."
        )
      )
    )
  }

}
