/*
 * Part of NDLA language
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.language

import no.ndla.language.model.{LanguageField, WithLanguage}

class LanguageTest extends UnitSuite {

  case class ArticleTitle(title: String, language: String) extends LanguageField[String] {
    override def value: String    = title
    override def isEmpty: Boolean = title.isEmpty
  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing =
      Seq(ArticleTitle("Tittel 1", "nb"), ArticleTitle("Tittel 2", "nn"), ArticleTitle("Tittel 3", "und"))
    Language.mergeLanguageFields(existing, Seq()) should equal(existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1          = ArticleTitle("Tittel 1", "nb")
    val tittel2          = ArticleTitle("Tittel 2", "nn")
    val tittel3          = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel3 = ArticleTitle("Title 3 in english", "en")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated  = Seq(oppdatertTittel3)

    Language.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1        = ArticleTitle("Tittel 1", "nb")
    val tittel2        = ArticleTitle("Tittel 2", "nn")
    val tittel3        = ArticleTitle("Tittel 3", "en")
    val tittelToRemove = ArticleTitle("", "nn")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated  = Seq(tittelToRemove)

    Language.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with unknown language specified") {
    val tittel1          = ArticleTitle("Tittel 1", "nb")
    val tittel2          = ArticleTitle("Tittel 2", "und")
    val tittel3          = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel2 = ArticleTitle("Tittel 2 er oppdatert", "und")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated  = Seq(oppdatertTittel2)

    Language.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1          = ArticleTitle("Beskrivelse 1", "nb")
    val desc2          = ArticleTitle("Beskrivelse 2", "und")
    val desc3          = ArticleTitle("Beskrivelse 3", "en")
    val oppdatertDesc2 = ArticleTitle("Beskrivelse 2 er oppdatert", "und")

    val existing = Seq(desc1, desc2, desc3)
    val updated  = Seq(oppdatertDesc2)

    Language.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
  }

  test("That sorting sorts by language in prioritized order") {
    case class LangClass(x: String, language: String) extends WithLanguage

    val nb      = LangClass("hei", "nb")
    val und     = LangClass("kwargos", "und")
    val en      = LangClass("hello", "en")
    val de      = LangClass("hallo", "de")
    val sv      = LangClass("hej", "sv")
    val apekatt = LangClass("finnes ikke", "apekatt")

    val l = Seq(
      en,
      nb,
      und,
      de,
      apekatt,
      sv
    )

    Language.sortByLanguagePriority(l) should be(
      Seq(
        nb,
        en,
        de,
        sv,
        und,
        apekatt
      )
    )

  }
}
