/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.network.model.NdlaHttpRequest

object TaxonomyData {
  val TAXONOMY_VERSION_HEADER = "VersionHash"
  private val taxonomyVersion = new ThreadLocal[String]
  val defaultVersion          = "default"

  def set(value: String): Unit = {
    val taxonomyVersionValue = Option(value).getOrElse(defaultVersion)
    taxonomyVersion.set(taxonomyVersionValue)
  }

  def fromRequest(request: NdlaHttpRequest): String = {
    request.getHeader(TAXONOMY_VERSION_HEADER).getOrElse(defaultVersion)
  }

  def get: String = Option(taxonomyVersion.get()).getOrElse(defaultVersion)

  def clear(): Unit = taxonomyVersion.remove()

}
