/*
 * Part of NDLA validation
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
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

  test("validate should allow advanced math") {
    {
      val mathContent =
        "<section><p>Formel: <math><math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mfenced open=\"[\" close=\"]\"><mtable><mtr><mtd><mn>2</mn><mi>x</mi><mo>+</mo><mn>3</mn><mi>y</mi><mo>+</mo><mn>4</mn><mi>z</mi><mo>+</mo><mn>4</mn><mo>=</mo><mn>0</mn></mtd></mtr><mtr><mtd><mn>6</mn><mi>x</mi><mo>-</mo><mn>7</mn><mi>y</mi><mo>-</mo><mn>8</mn><mi>z</mi><mo>-</mo><mn>4</mn><mo>=</mo><mn>0</mn></mtd></mtr></mtable></mfenced></math></math></p></section>"
      val messages =
        TextValidator.validate(fieldPath = "content", text = mathContent, HtmlTagRules.allLegalTags)
      messages.length should be(0)
    }
    {
      val mathContent =
        "<section><p><math><math xmlns=\"http://www.w3.org/1998/Math/MathML\"><msub><mover><mi>n</mi><mo stretchy=\"false\">→</mo></mover><mi>β</mi></msub></math></math> til <math><math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mi>β</mi></math></math>.</p><p data-align=\"center\"><math><math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mtable columnspacing=\"0px\" columnalign=\"right center left\"><mtr><mtd><msub><mover><mi>n</mi><mo stretchy=\"false\">→</mo></mover><mi>α</mi></msub><mo>&#xa0;</mo></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>2</mn><mo>,</mo><mn>3</mn><mo>,</mo><mn>4</mn></mrow></mfenced><mo>,</mo><mo>&#xa0;</mo><mo>&#xa0;</mo><msub><mover><mi>n</mi><mo stretchy=\"false\">→</mo></mover><mi>β</mi></msub><mo>=</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>6</mn><mo>,</mo><mo>-</mo><mn>7</mn><mo>,</mo><mo>-</mo><mn>8</mn></mrow></mfenced></mtd></mtr><mtr><mtd></mtd><mtd></mtd><mtd></mtd></mtr><mtr><mtd><msub><mover><mi>n</mi><mo stretchy=\"false\">→</mo></mover><mi>α</mi></msub><mo>×</mo><msub><mover><mi>n</mi><mo stretchy=\"false\">→</mo></mover><mi>β</mi></msub><mo>&#xa0;</mo></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>2</mn><mo>,</mo><mn>3</mn><mo>,</mo><mn>4</mn></mrow></mfenced><mo>×</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>6</mn><mo>,</mo><mo>-</mo><mn>7</mn><mo>,</mo><mo>-</mo><mn>8</mn></mrow></mfenced></mtd></mtr><mtr><mtd></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>3</mn><mo>·</mo><mfenced><mrow><mo>-</mo><mn>8</mn></mrow></mfenced><mo>-</mo><mfenced><mrow><mo>-</mo><mn>7</mn></mrow></mfenced><mo>·</mo><mn>4</mn><mo>,</mo><mn>4</mn><mo>·</mo><mn>6</mn><mo>-</mo><mfenced><mrow><mo>-</mo><mn>8</mn></mrow></mfenced><mo>·</mo><mn>2</mn><mo>,</mo><mn>2</mn><mo>·</mo><mfenced><mrow><mo>-</mo><mn>7</mn></mrow></mfenced><mo>-</mo><mn>6</mn><mo>·</mo><mn>3</mn></mrow></mfenced></mtd></mtr><mtr><mtd></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mfenced open=\"[\" close=\"]\"><mrow><mo>-</mo><mn>24</mn><mo>+</mo><mn>28</mn><mo>,</mo><mn>24</mn><mo>+</mo><mn>16</mn><mo>,</mo><mo>-</mo><mn>14</mn><mo>-</mo><mn>18</mn></mrow></mfenced></mtd></mtr><mtr><mtd></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mfenced open=\"[\" close=\"]\"><mrow><mn>4</mn><mo>,</mo><mn>40</mn><mo>,</mo><mo>-</mo><mn>32</mn></mrow></mfenced></mtd></mtr><mtr><mtd></mtd><mtd><mo>=</mo></mtd><mtd><mo>&#xa0;</mo><mn>4</mn><mfenced open=\"[\" close=\"]\"><mrow><mn>1</mn><mo>,</mo><mn>10</mn><mo>,</mo><mo>-</mo><mn>8</mn></mrow></mfenced></mtd></mtr></mtable></math></math></p></section>"
      val messages =
        TextValidator.validate(fieldPath = "content", text = mathContent, HtmlTagRules.allLegalTags)
      messages.length should be(0)
    }
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
