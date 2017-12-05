/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

class LanguageNotSupportedException(message: String) extends RuntimeException(message)
class LanguageSubtagNotSupportedException(languageSubTag: String) extends  LanguageNotSupportedException(s"The language subtag '$languageSubTag' is not supported.")
class ScriptSubtagNotSupportedException(scriptSubTag: String) extends LanguageNotSupportedException(s"The script subtag '$scriptSubTag' is not supported.")
class RegionSubtagNotSupportedException(regionSubtag: String) extends LanguageNotSupportedException(s"The region subtag '$regionSubtag' is not supported.")
