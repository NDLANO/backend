/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.common.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    context.mount(componentRegistry.searchController, "/search-api/v1/search", "search")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.resourcesApp, "/search-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
  }
}
