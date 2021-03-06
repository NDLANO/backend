/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.CorrelationID
import no.ndla.oembedproxy.Props
import org.apache.logging.log4j.ThreadContext
import org.scalatra.CoreDsl

trait CorrelationIdSupport {
  this: Props =>

  trait CorrelationIdSupport extends CoreDsl with LazyLogging {
    import props.{CorrelationIdKey, CorrelationIdHeader}

    before() {
      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
    }

  }
}
