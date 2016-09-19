/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import org.apache.logging.log4j.ThreadContext
import org.scalatra.CoreDsl
import no.ndla.audioapi.AudioApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.network.CorrelationID

trait CorrelationIdSupport extends CoreDsl {

  before() {
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
  }

}
