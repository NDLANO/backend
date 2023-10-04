package no.ndla.validation

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationMessage

class HtmlValidatorTest extends UnitSuite {

  private val getValidImageEmbed = (id: String) => {
    s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="$id" data-resource="image" data-alt="some alt" data-size="full" />"""
  }

  test("validate should allow math tags with styling") {
    val mathContent =
      "<section><p>Formel: <math style=\"font-family:'Courier New'\" xmlns=\"http://www.w3.org/1998/Math/MathML\"><mmultiscripts><mn>22</mn><mprescripts/><mn>22</mn><mn>22</mn></mmultiscripts><mo>&#xA0;</mo><mi>h</mi><mi>a</mi><mi>l</mi><mi>l</mi><mi>o</mi><mrow style=\"font-family:'Courier New'\"><mi>a</mi><mi>s</mi><mi>d</mi><mi>f</mi></mrow></math></p></section>"
    val messages =
      TextValidator.validate(fieldPath = "content", text = mathContent, HtmlTagRules.allLegalTags)
    messages.length should be(0)
  }

  test("Validating visual elements should fail if the tag is not an embed") {
    TextValidator.validateVisualElement("test", "") should be(
      Seq(ValidationMessage("test", "The root html element for visual elements needs to be `embed`."))
    )
    TextValidator.validateVisualElement("test", "apekatt") should be(
      Seq(ValidationMessage("test", "The root html element for visual elements needs to be `embed`."))
    )
  }

  test("Passing multiple embeds when validating visual element should fail") {
    TextValidator.validateVisualElement(
      "test",
      s"""${getValidImageEmbed("1")}${getValidImageEmbed("2")}"""
    ) should be(
      Seq(ValidationMessage("test", "Visual element must be a string containing only a single embed element."))
    )
  }

  test("Passing a single valid embed should work") {
    TextValidator.validateVisualElement(
      "test",
      getValidImageEmbed("1")
    ) should be(
      Seq.empty
    )
  }
}
