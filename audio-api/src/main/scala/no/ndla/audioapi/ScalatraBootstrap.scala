/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.audioapi

import no.ndla.network.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    context.mount(componentRegistry.audioApiController, "/audio-api/v1/audio")
    context.mount(componentRegistry.seriesController, "/audio-api/v1/series")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.resourcesApp, "/audio-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
  }
}
