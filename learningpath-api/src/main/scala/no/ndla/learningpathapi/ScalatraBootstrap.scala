package no.ndla.learningpathapi

/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[ComponentRegistry]
    context.mount(componentRegistry.learningpathControllerV2, "/learningpath-api/v2/learningpaths", "learningpaths_v2")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.resourcesApp, "/learningpath-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
    context.mount(componentRegistry.configController, "/learningpath-api/v1/config", "config")
  }
}
