/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.configuration.{BaseProps, HasBaseProps, Prop}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: FrontpageApiProperties
}

class FrontpageApiProperties extends BaseProps with DatabaseProps {
  def ApplicationName            = "frontpage-api"
  val ApplicationPort: Int       = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String    = propOrElse("DEFAULT_LANGUAGE", "nb")
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val DefaultPageSize        = 10
  val Domain: String         = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val RawImageApiUrl: String = s"$Domain/image-api/raw"

  val BrightcoveAccountId: Prop = prop("BRIGHTCOVE_ACCOUNT_ID")
  val BrightcovePlayer: Prop    = prop("BRIGHTCOVE_PLAYER_ID")

  override def MetaMigrationLocation: String = "no/ndla/frontpageapi/db/migration"
}
