package no.ndla.draftapi

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[ComponentRegistry]
    context.mount(componentRegistry.draftController, "/draft-api/v1/drafts", "drafts")
    context.mount(componentRegistry.fileController, "/draft-api/v1/files", "files")
    context.mount(componentRegistry.agreementController, "/draft-api/v1/agreements/", "agreements")
    context.mount(componentRegistry.ruleController, "/draft-api/v1/rules", "rules")
    context.mount(componentRegistry.userDataController, "/draft-api/v1/user-data", "user-data")
    context.mount(componentRegistry.resourcesApp, "/draft-api/api-docs")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, "/health")
  }

}
