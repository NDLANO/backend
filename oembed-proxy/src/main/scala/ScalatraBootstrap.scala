/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.oembedproxy.{ComponentRegistry, OEmbedProxyProperties}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    context.mount(ComponentRegistry.oEmbedProxyController, OEmbedProxyProperties.OembedProxyControllerMountPoint)
    context.mount(ComponentRegistry.resourcesApp, OEmbedProxyProperties.ResourcesAppMountPoint)
    context.mount(ComponentRegistry.healthController, OEmbedProxyProperties.HealthControllerMountPoint)
  }
}
