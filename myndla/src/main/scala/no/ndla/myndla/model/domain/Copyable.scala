/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

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
  val resourceType: String
  val path: String
  val tags: List[String]
  val resourceId: String
}
