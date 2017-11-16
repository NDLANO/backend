/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

trait LanguageSupport {
  implicit val languageProvider: LanguageProvider
}