/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.network.model.NdlaHttpRequest
import org.http4s.Request
import org.typelevel.ci.CIString

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

  def getFromRequest[F[_]](request: Request[F]): String =
    request.headers
      .get(CIString(TAXONOMY_VERSION_HEADER))
      .map(_.head.value)
      .getOrElse(defaultVersion)

  def get: String = Option(taxonomyVersion.get()).getOrElse(defaultVersion)

  def clear(): Unit = taxonomyVersion.remove()

}
