/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext
import no.ndla.articleapi.JettyLauncher.ComponentRegistry.{
  articleControllerV2,
  healthController,
  internController,
  resourcesApp
}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    context.mount(articleControllerV2, "/article-api/v2/articles", "articlesV2")
    context.mount(resourcesApp, "/article-api/api-docs")
    context.mount(internController, "/intern")
    context.mount(healthController, "/health")
  }

}
