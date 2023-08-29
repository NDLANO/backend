package no.ndla.draftapi

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

import no.ndla.network.scalatra.NdlaScalatraBootstrapBase
import javax.servlet.ServletContext

class ScalatraBootstrap extends NdlaScalatraBootstrapBase[ComponentRegistry] {
  override def ndlaInit(context: ServletContext, componentRegistry: ComponentRegistry): Unit = {
    context.mount(componentRegistry.draftController, "/draft-api/v1/drafts", "drafts")
    context.mount(componentRegistry.fileController, "/draft-api/v1/files", "files")
    context.mount(componentRegistry.ruleController, "/draft-api/v1/rules", "rules")
    context.mount(componentRegistry.userDataController, "/draft-api/v1/user-data", "user-data")
    context.mount(componentRegistry.resourcesApp, "/draft-api/api-docs")
    context.mount(componentRegistry.internController, "/intern")
    context.mount(componentRegistry.healthController, "/health")
  }
}
