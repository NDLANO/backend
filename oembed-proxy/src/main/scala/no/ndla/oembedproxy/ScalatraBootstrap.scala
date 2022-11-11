/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.oembedproxy

import no.ndla.common.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    import componentRegistry.props.{OembedProxyControllerMountPoint, ResourcesAppMountPoint, HealthControllerMountPoint}

    context.mount(componentRegistry.oEmbedProxyController, OembedProxyControllerMountPoint)
    context.mount(componentRegistry.resourcesApp, ResourcesAppMountPoint)
    context.mount(componentRegistry.healthController, HealthControllerMountPoint)
  }
}
