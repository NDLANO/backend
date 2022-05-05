/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi

import org.scalatra.LifeCycle
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[ComponentRegistry]
    context.mount(componentRegistry.draftConceptController, "/concept-api/v1/drafts", "concept")
    context.mount(componentRegistry.publishedConceptController, "/concept-api/v1/concepts", "publishedConcept")
    context.mount(componentRegistry.resourcesApp, "/concept-api/api-docs")
    context.mount(componentRegistry.healthController, "/health")
    context.mount(componentRegistry.internController, "/intern")
  }
}
