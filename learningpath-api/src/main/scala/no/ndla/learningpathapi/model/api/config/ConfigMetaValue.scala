/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api.config

import no.ndla.learningpathapi.model.domain
import org.scalatra.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ConfigMetaValue(
    @(ApiModelProperty @field)(description = "Value to set configuration param to.")
    value: Either[Boolean, List[String]]
)

object ConfigMetaValue {
  def from(value: domain.config.ConfigMetaValue): ConfigMetaValue = {
    value match {
      case domain.config.BooleanValue(value)    => ConfigMetaValue(Left(value))
      case domain.config.StringListValue(value) => ConfigMetaValue(Right(value))
    }
  }

  def apply(value: Boolean): ConfigMetaValue      = ConfigMetaValue(Left(value))
  def apply(value: List[String]): ConfigMetaValue = ConfigMetaValue(Right(value))
}
