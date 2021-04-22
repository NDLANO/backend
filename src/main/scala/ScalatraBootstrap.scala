/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.audioapi.ComponentRegistry
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    context.mount(ComponentRegistry.audioApiController, "/audio-api/v1/audio")
    context.mount(ComponentRegistry.seriesController, "/audio-api/v1/series")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.resourcesApp, "/audio-api/api-docs")
    context.mount(ComponentRegistry.healthController, "/health")
  }
}
