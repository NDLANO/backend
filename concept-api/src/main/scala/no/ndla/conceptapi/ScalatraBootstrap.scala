/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi

import no.ndla.common.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    context.mount(componentRegistry.draftConceptController, "/concept-api/v1/drafts", "concept")
    context.mount(componentRegistry.publishedConceptController, "/concept-api/v1/concepts", "publishedConcept")
    context.mount(componentRegistry.resourcesApp, "/concept-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
    context.mount(componentRegistry.internController, "/intern")
  }
}
