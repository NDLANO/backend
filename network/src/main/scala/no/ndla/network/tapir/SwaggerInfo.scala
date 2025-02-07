/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import sttp.tapir.EndpointInput

import scala.collection.immutable.ListMap

case class SwaggerInfo(
    mountPoint: EndpointInput[Unit],
    description: String,
    authUrl: String,
    scopes: ListMap[String, String]
)
