package no.ndla.validation

import no.ndla.mapping.UnitSuite

class HtmlValidatorTest extends UnitSuite {
  val htmlValidator = new TextValidator(allowHtml = true)

  test("validate should allow math tags with styling") {
    val mathContent =
      "<section><p>Formel: <math style=\"font-family:'Courier New'\" xmlns=\"http://www.w3.org/1998/Math/MathML\"><mmultiscripts><mn>22</mn><mprescripts/><mn>22</mn><mn>22</mn></mmultiscripts><mo>&#xA0;</mo><mi>h</mi><mi>a</mi><mi>l</mi><mi>l</mi><mi>o</mi><mrow style=\"font-family:'Courier New'\"><mi>a</mi><mi>s</mi><mi>d</mi><mi>f</mi></mrow></math></p></section>"
    val messages =
      htmlValidator.validate(fieldPath = "content", text = mathContent)
    messages.length should be(0)
  }
}
