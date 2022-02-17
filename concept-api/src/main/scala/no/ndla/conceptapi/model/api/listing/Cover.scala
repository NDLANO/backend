/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api.listing

import no.ndla.language.model.LanguageField

import java.util.Date

case class Cover(
    id: Option[Long],
    revision: Option[Int],
    oldNodeId: Option[Long],
    coverPhotoUrl: String,
    title: Seq[CoverTitle],
    description: Seq[CoverDescription],
    labels: Seq[CoverLanguageLabels],
    articleApiId: Long,
    updatedBy: String,
    updated: Date,
    theme: String
) {
  lazy val supportedLanguages: Set[String] =
    (title concat description concat labels).map(_.language).toSet
}

case class CoverDescription(description: String, language: String) extends LanguageField[String] {
  def value: String    = description
  def isEmpty: Boolean = description.isEmpty
}
case class CoverTitle(title: String, language: String) extends LanguageField[String] {
  def value: String    = title
  def isEmpty: Boolean = title.isEmpty
}
case class CoverLanguageLabels(labels: Seq[CoverLabel], language: String) extends LanguageField[Seq[CoverLabel]] {
  def value: Seq[CoverLabel] = labels
  def isEmpty: Boolean       = labels.isEmpty
}

case class CoverLabel(`type`: Option[String], labels: Seq[String])
