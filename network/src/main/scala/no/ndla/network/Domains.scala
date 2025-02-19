/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.network

object Domains {

  def get(environment: String): String =
    Map(
      "local" -> "http://api-gateway.ndla-local",
      "prod"  -> "https://api.ndla.no"
    ).getOrElse(environment, s"https://api.${environment.replace('_', '-')}.ndla.no")

}
