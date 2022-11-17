/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common.scalatra

import no.ndla.common.Warmup
import org.scalatra._

class BaseHealthController extends ScalatraServlet with Warmup {

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
  }
}
