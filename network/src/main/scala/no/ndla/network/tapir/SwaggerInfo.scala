package no.ndla.network.tapir

import scala.collection.immutable.ListMap

case class SwaggerInfo(
    mountPoint: String,
    description: String,
    authUrl: String,
    scopes: ListMap[String, String]
)
