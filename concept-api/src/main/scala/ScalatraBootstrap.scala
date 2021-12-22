/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

import javax.servlet.ServletContext
import no.ndla.conceptapi.ComponentRegistry.{
  draftConceptController,
  publishedConceptController,
  resourcesApp,
  healthController,
  internController
}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    context.mount(draftConceptController, "/concept-api/v1/drafts", "concept")
    context.mount(publishedConceptController, "/concept-api/v1/concepts", "publishedConcept")
    context.mount(resourcesApp, "/concept-api/api-docs")
    context.mount(healthController, "/health")
    context.mount(internController, "/intern")
  }
}
