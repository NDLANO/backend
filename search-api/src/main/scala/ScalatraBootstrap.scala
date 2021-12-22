/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import no.ndla.searchapi.ComponentRegistry.{healthController, resourcesApp, searchController, internController}

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    context.mount(searchController, "/search-api/v1/search", "search")
    context.mount(internController, "/intern")
    context.mount(resourcesApp, "/search-api/api-docs")
    context.mount(healthController, "/health")
  }

}
