/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import sttp.client4.Request

package object model {
  type FeideID          = String
  type FeideAccessToken = String
  type NdlaRequest      = Request[String]
}
