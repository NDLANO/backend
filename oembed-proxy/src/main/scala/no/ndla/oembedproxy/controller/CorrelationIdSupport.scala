/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.CorrelationID
import no.ndla.oembedproxy.OEmbedProxyProperties.{CorrelationIdHeader, CorrelationIdKey}
import org.apache.logging.log4j.ThreadContext
import org.scalatra.CoreDsl

trait CorrelationIdSupport extends CoreDsl with LazyLogging {

  before() {
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
  }

}
