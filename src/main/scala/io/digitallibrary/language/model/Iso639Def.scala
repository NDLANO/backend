/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

case class Iso639Def(id: String,
                     part2b: Option[String],
                     part2t: Option[String],
                     part1: Option[String],
                     scope: Option[String],
                     languageType: Option[String],
                     refName: String,
                     comment: Option[String])

