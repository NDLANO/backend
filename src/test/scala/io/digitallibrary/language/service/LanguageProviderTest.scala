/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.{TestData, UnitSuite}
import io.digitallibrary.language.model._
import org.mockito.Mockito
import org.mockito.Mockito._


class LanguageProviderTest extends UnitSuite {

  private val iso639Mock = mock[Iso639]
  private val iso3166Mock = mock[Iso3166]
  private val iso15924Mock = mock[Iso15924]
  private val languageProvider = new LanguageProvider(iso639Mock, iso3166Mock, iso15924Mock)

  override def beforeEach(): Unit = {
    Mockito.reset(iso639Mock, iso3166Mock, iso15924Mock)
  }

  test("that LanguageSubtagNotSupportedException is thrown when invalid iso639-code") {
    when(iso639Mock.findAlpha3(TestData.DefaultLanguageTag.language)).thenReturn(None)

    intercept[LanguageSubtagNotSupportedException] {
      languageProvider.validate(TestData.DefaultLanguageTag)
    }.getMessage
      .contains(TestData.DefaultLanguageTag.language) should be(true)
  }

  test("that ScriptSubtagNotSupportedException is thrown when invalid iso15924-code") {
    when(iso639Mock.findAlpha3(TestData.DefaultLanguageTag.language)).thenReturn(Some(TestData.DefaultIso639Def))
    when(iso15924Mock.findAlpha4(TestData.DefaultLanguageTag.script.get)).thenReturn(None)

    intercept[ScriptSubtagNotSupportedException] {
      languageProvider.validate(TestData.DefaultLanguageTag)
    }.getMessage
      .contains(TestData.DefaultLanguageTag.script.get) should be(true)
  }

  test("that RegionSubtagNotSupportedException is thrown when invalid iso3166-code") {
    when(iso639Mock.findAlpha3(TestData.DefaultLanguageTag.language)).thenReturn(Some(TestData.DefaultIso639Def))
    when(iso15924Mock.findAlpha4(TestData.DefaultLanguageTag.script.get)).thenReturn(Some(TestData.DefaultIso15924Def))
    when(iso3166Mock.findAlpha2(TestData.DefaultLanguageTag.region.get)).thenReturn(None)

    intercept[RegionSubtagNotSupportedException] {
      languageProvider.validate(TestData.DefaultLanguageTag)
    }.getMessage
      .contains(TestData.DefaultLanguageTag.region.get) should be(true)
  }

  test("that LanguageTag is returned when valid language tag") {
    when(iso639Mock.findAlpha3(TestData.DefaultLanguageTag.language)).thenReturn(Some(TestData.DefaultIso639Def))
    when(iso15924Mock.findAlpha4(TestData.DefaultLanguageTag.script.get)).thenReturn(Some(TestData.DefaultIso15924Def))
    when(iso3166Mock.findAlpha2(TestData.DefaultLanguageTag.region.get)).thenReturn(Some(TestData.DefaultIso3166Def))

    val languageTag = languageProvider.validate(TestData.DefaultLanguageTag)

    languageTag.toString should equal(TestData.DefaultLanguageTag.toString)
  }

  test("that displayName only returns 'language' when no script or region subtag"){
    val tag = LanguageTag(TestData.DefaultLanguageTag.language)
    when(iso639Mock.findAlpha3(tag.language)).thenReturn(Some(TestData.DefaultIso639Def))

    languageProvider.displayName(tag) should equal (Some(TestData.DefaultIso639Def.refName))
  }

  test("that displayName returns 'language (script)' when no region subtag"){
    val tag = LanguageTag("amh-ethi")
    when(iso639Mock.findAlpha3(tag.language)).thenReturn(Some(TestData.DefaultIso639Def.copy(refName = "Amharic")))
    when(iso15924Mock.findAlpha4(tag.script.get)).thenReturn(Some(TestData.DefaultIso15924Def.copy(englishName = "My-script")))

    languageProvider.displayName(tag) should equal (Some("Amharic (My-script)"))
  }

  test("that displayName returns 'language (region)' when no script subtag"){
    val tag = LanguageTag("amh-et")
    when(iso639Mock.findAlpha3(tag.language)).thenReturn(Some(TestData.DefaultIso639Def.copy(refName = "Amharic")))
    when(iso3166Mock.findAlpha2(tag.region.get)).thenReturn(Some(TestData.DefaultIso3166Def.copy(name = "My-region")))

    languageProvider.displayName(tag) should equal (Some("Amharic (My-region)"))
  }

  test("that displayName returns 'language (script, region)' when all defined"){
    val tag = LanguageTag("amh-ethi-et")
    when(iso639Mock.findAlpha3(tag.language)).thenReturn(Some(TestData.DefaultIso639Def.copy(refName = "Amharic")))
    when(iso15924Mock.findAlpha4(tag.script.get)).thenReturn(Some(TestData.DefaultIso15924Def.copy(englishName = "My-script")))
    when(iso3166Mock.findAlpha2(tag.region.get)).thenReturn(Some(TestData.DefaultIso3166Def.copy(name = "My-region")))

    languageProvider.displayName(tag) should equal (Some("Amharic (My-script, My-region)"))
  }

  test("that withIso639_3 throws exception for an invalid 2 letter code") {
    when(iso639Mock.findAlpha3("x")).thenReturn(None)

    intercept[LanguageSubtagNotSupportedException] {
      languageProvider.withIso639_3(TestData.DefaultLanguageTag.copy(language = "x"))
    }.getMessage
      .contains("'x'") should be(true)
  }

  test("that withIso639_3 returns a languageTag with 3-letter language code for a valid 2/3-letter language code") {
    val languageCode = TestData.DefaultIso639Def.part1.get

    when(iso639Mock.findAlpha3(languageCode)).thenReturn(Some(TestData.DefaultIso639Def))
    val with3Letter = languageProvider.withIso639_3(TestData.DefaultLanguageTag.copy(language = languageCode))

    with3Letter.language should equal (TestData.DefaultIso639Def.id)
  }
}
