/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V59__EnsureWrappingSectionTest extends UnitSuite with TestEnvironment {
  test("That the content should be wrapped in a <section> if none exists") {
    val migration  = new V59__EnsureWrappingSection
    val oldArticle = """<p>Dette er en forklaringsartikkel.</p><p>Som forklarer både det ene og det andre.</p>"""
    val newArticle =
      """<section><p>Dette er en forklaringsartikkel.</p><p>Som forklarer både det ene og det andre.</p></section>"""

    migration.convertContent(oldArticle, "nb") should be(newArticle)
  }

  test("That the content should not be wrapped if there exists a <section>") {
    val migration  = new V59__EnsureWrappingSection
    val validArticle = """<section>Dette er en forklaringsartikkel.</section><p>Her er et paragraf.</p>"""

    migration.convertContent(validArticle, "nb") should be(validArticle)
  }
}
