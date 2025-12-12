/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.configuration.{BaseProps, Prop}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.database.DatabaseProps

import scala.util.Properties.*

type Props = FrontpageApiProperties

class FrontpageApiProperties extends BaseProps with DatabaseProps {
  def ApplicationName            = "frontpage-api"
  val ApplicationPort: Int       = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String    = propOrElse("DEFAULT_LANGUAGE", "nb")
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val DefaultPageSize        = 10
  val Domain: String         = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val RawImageApiUrl: String = propOrElse("IMAGE_API_CLOUDFRONT_DOMAIN", s"$Domain/image-api/raw")

  val BrightcoveAccountId: Prop[String] = prop("BRIGHTCOVE_ACCOUNT_ID")
  val BrightcovePlayer: Prop[String]    = prop("BRIGHTCOVE_PLAYER_ID")

  override def MetaMigrationLocation: String = "no/ndla/frontpageapi/db/migration"
}
