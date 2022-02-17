/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

sealed trait TaxonomyElement {
  val id: String
  val name: String
  val contentUri: Option[String]
  val path: Option[String]
  val metadata: Option[Metadata]
  val translations: List[TaxonomyTranslation]

  def getNameFromTranslationOrDefault(language: String): String = {
    translations.find(_.language == language).map(_.name).getOrElse(name)
  }
}

case class TaxonomyTranslation(name: String, language: String)

case class TaxSubject(
    id: String,
    name: String,
    contentUri: Option[String],
    path: Option[String],
    metadata: Option[Metadata],
    translations: List[TaxonomyTranslation],
) extends TaxonomyElement

case class Resource(
    id: String,
    name: String,
    contentUri: Option[String],
    path: Option[String],
    metadata: Option[Metadata],
    translations: List[TaxonomyTranslation]
) extends TaxonomyElement

case class Topic(
    id: String,
    name: String,
    contentUri: Option[String],
    path: Option[String],
    metadata: Option[Metadata],
    translations: List[TaxonomyTranslation]
) extends TaxonomyElement
