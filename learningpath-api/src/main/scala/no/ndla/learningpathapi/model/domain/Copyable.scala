/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

/** Traits that contains fields that are required for copying a folder and its resources. Used so we can generalize the
  * copy method for both api and domain input.
  */
trait CopyableFolder {
  val name: String
  val subfolders: List[CopyableFolder]
  val resources: List[CopyableResource]
  val rank: Option[Int]
}

trait CopyableResource {
  val resourceType: String
  val path: String
  val tags: List[String]
  val resourceId: Long
}
