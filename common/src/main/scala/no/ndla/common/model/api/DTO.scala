/*
 * Part of NDLA common.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import sttp.tapir.Schema.annotations.description

trait ApiType[T <: DomainType[?]] {
  def toDomain: T
  val language: String
}

trait DomainType[T <: ApiType[?]] {
  def toApi: T
  val language: String
}

case class ApiTitle(
    @description("The title of the resource") title: String,
    @description("The language") override val language: String
) extends ApiType[DomainTitle] {
  override def toDomain: DomainTitle = DomainTitle(title, language)
}

case class DomainTitle(title: String, override val language: String) extends DomainType[ApiTitle] {
  override def toApi: ApiTitle = ApiTitle(title, language)
}

case class FakeArticle(
    @description("The title of the resource") title: ApiTitle
)
