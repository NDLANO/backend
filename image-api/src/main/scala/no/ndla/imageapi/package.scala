/*
 * Part of NDLA image-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object imageapi {
  type Eff[A] = Id[A]
}
