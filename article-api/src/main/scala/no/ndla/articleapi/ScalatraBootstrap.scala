package no.ndla.articleapi

/*
 * Part of NDLA article-api.
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

    context.mount(componentRegistry.articleControllerV2, "/article-api/v2/articles", "articlesV2")
    context.mount(componentRegistry.resourcesApp, "/article-api/api-docs")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, "/health")
  }

}
