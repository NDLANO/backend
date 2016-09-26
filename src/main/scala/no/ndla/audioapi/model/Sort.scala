/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model

object Sort  extends Enumeration {
  val ByRelevanceDesc = Value("-relevance")
  val ByRelevanceAsc = Value("relevance")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")

  def valueOf(s:String): Option[Sort.Value] = {
    Sort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[Sort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}

