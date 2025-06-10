/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

case class BulkInserts(
    folders: List[Folder],
    resources: List[Resource],
    connections: List[FolderResource]
) {
  def addFolder(folder: Folder): BulkInserts                 = this.copy(folders = this.folders :+ folder)
  def addResource(resource: Resource): BulkInserts           = this.copy(resources = this.resources :+ resource)
  def addConnection(connection: FolderResource): BulkInserts = this.copy(connections = this.connections :+ connection)
}

object BulkInserts {
  def empty: BulkInserts = BulkInserts(List.empty, List.empty, List.empty)
}
