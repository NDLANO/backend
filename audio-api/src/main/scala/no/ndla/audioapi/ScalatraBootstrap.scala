/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.audioapi

import org.scalatra.LifeCycle
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[ComponentRegistry]
    context.mount(componentRegistry.audioApiController, "/audio-api/v1/audio")
    context.mount(componentRegistry.seriesController, "/audio-api/v1/series")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.resourcesApp, "/audio-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
  }
}
