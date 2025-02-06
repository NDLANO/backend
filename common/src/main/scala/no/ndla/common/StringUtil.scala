/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

object StringUtil {
  def emptySomeToNone(s: Option[String]): Option[String] = s.filter(_.nonEmpty)
}
