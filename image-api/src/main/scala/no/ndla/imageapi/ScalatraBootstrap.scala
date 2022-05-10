package no.ndla.imageapi

/*
 * Part of NDLA image-api.
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
    import componentRegistry.props.{ImageApiBasePath, RawControllerPath, ApiDocsPath, HealthControllerPath}

    context.mount(componentRegistry.imageControllerV2, s"${ImageApiBasePath}/v2/images", "imagesV2")
    context.mount(componentRegistry.rawController, RawControllerPath, "raw")
    context.mount(componentRegistry.resourcesApp, ApiDocsPath)
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, HealthControllerPath)
  }
}
