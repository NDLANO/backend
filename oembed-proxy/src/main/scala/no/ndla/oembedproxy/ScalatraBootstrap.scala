/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.oembedproxy

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[ComponentRegistry]
    import componentRegistry.props.{OembedProxyControllerMountPoint, ResourcesAppMountPoint, HealthControllerMountPoint}

    context.mount(componentRegistry.oEmbedProxyController, OembedProxyControllerMountPoint)
    context.mount(componentRegistry.resourcesApp, ResourcesAppMountPoint)
    context.mount(componentRegistry.healthController, HealthControllerMountPoint)
  }
}
