/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.oembedproxy.OEmbedProxyProperties
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object Error {
  val GENERIC = "GENERIC"
  val PARAMETER_MISSING = "PARAMETER MISSING"
  val PROVIDER_NOT_SUPPORTED = "PROVIDER NOT SUPPORTED"
  val REMOTE_ERROR = "REMOTE ERROR"

  val GenericError: Error = Error(
    GENERIC,
    s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${OEmbedProxyProperties.ContactEmail} if the error persists."
  )
}

@ApiModel(description = "Information about errors")
case class Error(@(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
                 @(ApiModelProperty @field)(description = "Description of the error") description: String,
                 @(ApiModelProperty @field)(description = "When the error occured") occuredAt: String =
                   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                     .format(new Date()))

class ParameterMissingException(message: String) extends RuntimeException(message)
case class ProviderNotSupportedException(message: String) extends RuntimeException(message)
class DoNotUpdateMemoizeException(message: String) extends RuntimeException(message)
