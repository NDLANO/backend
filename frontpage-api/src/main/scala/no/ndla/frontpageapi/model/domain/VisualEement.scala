/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.frontpageapi.model.domain.Errors.ValidationException

import scala.util.{Failure, Success, Try}

case class VisualElement(`type`: VisualElementType.Value, id: String, alt: Option[String])

object VisualElementType extends Enumeration {
  val Image: VisualElementType.Value = Value("image")
  val Brightcove: VisualElementType.Value = Value("brightcove")

  def fromString(str: String): Try[VisualElementType.Value] =
    VisualElementType.values.find(_.toString == str) match {
      case Some(v) => Success(v)
      case None    => Failure(ValidationException(s"'$str' is an invalid visual element type"))
    }
}
