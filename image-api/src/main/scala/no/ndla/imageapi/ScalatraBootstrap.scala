/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.imageapi

import no.ndla.common.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    import componentRegistry.props.{ApiDocsPath, HealthControllerPath, ImageApiBasePath, RawControllerPath}

    context.mount(componentRegistry.imageControllerV2, s"$ImageApiBasePath/v2/images", "imagesV2")
    context.mount(componentRegistry.imageControllerV3, s"$ImageApiBasePath/v3/images", "imagesV3")
    context.mount(componentRegistry.rawController, RawControllerPath, "raw")
    context.mount(componentRegistry.resourcesApp, ApiDocsPath)
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, HealthControllerPath)
  }
}
