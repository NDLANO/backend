/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.learningpathapi.ComponentRegistry
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.learningpathController, "/learningpath-api/v1/learningpaths", "learningpaths_v1")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.resourcesApp, "/api-docs")
    context.mount(ComponentRegistry.healthController, "/health")

  }
}
