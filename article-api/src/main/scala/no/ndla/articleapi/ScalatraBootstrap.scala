/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.network.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    context.mount(componentRegistry.articleControllerV2, "/article-api/v2/articles", "articlesV2")
    context.mount(componentRegistry.resourcesApp, "/article-api/api-docs")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, "/health")
  }
}
