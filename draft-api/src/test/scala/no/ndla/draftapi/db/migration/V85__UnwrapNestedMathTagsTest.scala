/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration
import no.ndla.draftapi.{TestEnvironment, UnitSuite}
class V85__UnwrapNestedMathTagsTest extends UnitSuite with TestEnvironment {
  val migration = new V85__UnwrapNestedMathTags

  test("That nested math tags are unwrapped while keeping the outermost math tag") {
    val oldArticle =
      """<section><p><math><math xmlns="http://www.w3.org/1998/Math/MathML"><mi>x</mi></math></math></p></section>"""
    val newArticle = """<section><p><math xmlns="http://www.w3.org/1998/Math/MathML"><mi>x</mi></math></p></section>"""
    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }

  test("That deep nested math tags are unwrapped while keeping the outermost math tag") {
    val oldArticle =
      """<section><p><math><math xmlns="http://www.w3.org/1998/Math/MathML"><mi><math xmlns="http://www.w3.org/1998/Math/MathML">x</math></mi></math></math></p></section>"""
    val newArticle = """<section><p><math xmlns="http://www.w3.org/1998/Math/MathML"><mi>x</mi></math></p></section>"""
    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }

  test("That deeply nested math tags are fully unwrapped and missing attributes are copied to the outermost tag") {
    val oldArticle =
      """<section><p><math display="block"><math xmlns="http://www.w3.org/1998/Math/MathML"><math style="color:red"><mi>x</mi></math><mo>+</mo><mi>y</mi></math></math></p></section>"""
    val newArticle =
      """<section><p><math display="block" xmlns="http://www.w3.org/1998/Math/MathML" style="color:red"><mi>x</mi><mo>+</mo><mi>y</mi></math></p></section>"""
    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }
}
