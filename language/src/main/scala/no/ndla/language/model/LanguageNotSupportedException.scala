/*
 * Part of NDLA backend
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

class LanguageNotSupportedException(message: String) extends RuntimeException(message)

case class LanguageSubtagNotSupportedException(languageSubTag: String)
    extends LanguageNotSupportedException(s"The language subtag '$languageSubTag' is not supported.")

case class ScriptSubtagNotSupportedException(scriptSubTag: String)
    extends LanguageNotSupportedException(s"The script subtag '$scriptSubTag' is not supported.")

case class RegionSubtagNotSupportedException(regionSubtag: String)
    extends LanguageNotSupportedException(s"The region subtag '$regionSubtag' is not supported.")
