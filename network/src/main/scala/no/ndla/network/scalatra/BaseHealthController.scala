/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.network.scalatra

import no.ndla.common.Warmup
import no.ndla.network.model.RequestInfo
import org.scalatra._

class BaseHealthController extends ScalatraServlet with Warmup {

  before() {
    RequestInfo.fromRequest(request).setRequestInfo()
  }

  protected def doIfWarmedUp(func: => Any): Any = {
    if (isWarmedUp) {
      func
    } else {
      InternalServerError("Warmup not finished")
    }
  }

  override def get(transformers: RouteTransformer*)(action: => Any): Route =
    addRoute(
      Get,
      transformers,
      doIfWarmedUp(action)
    )

  get("/") {
    Ok()
  }: Unit
}
