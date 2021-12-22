/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api.listing

import java.util.Date

case class Cover(id: Option[Long],
                 revision: Option[Int],
                 oldNodeId: Option[Long],
                 coverPhotoUrl: String,
                 title: Seq[CoverTitle],
                 description: Seq[CoverDescription],
                 labels: Seq[CoverLanguageLabels],
                 articleApiId: Long,
                 updatedBy: String,
                 updated: Date,
                 theme: String) {
  lazy val supportedLanguages: Set[String] =
    (title concat description concat labels).map(_.language).toSet
}

trait LanguageField[T] {
  val language: String
  def data: T
}

case class CoverDescription(description: String, language: String) extends LanguageField[String] {
  def data = description
}
case class CoverTitle(title: String, language: String) extends LanguageField[String] {
  def data = title
}
case class CoverLanguageLabels(labels: Seq[CoverLabel], language: String) extends LanguageField[Seq[CoverLabel]] {
  def data = labels
}

case class CoverLabel(`type`: Option[String], labels: Seq[String])
