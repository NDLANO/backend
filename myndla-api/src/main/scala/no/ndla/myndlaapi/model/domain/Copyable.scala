/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

/** Traits that contains fields that are required for copying a folder and its resources. Used so we can generalize the
  * copy method for both api and domain input.
  */
trait CopyableFolder {
  val name: String
  val description: Option[String]
  val subfolders: List[CopyableFolder]
  val resources: List[CopyableResource]
  val rank: Option[Int]
}

trait CopyableResource {
  val resourceType: ResourceType
  val path: String
  val tags: List[String]
  val resourceId: String
}
