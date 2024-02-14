/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object conceptapi {
  type Eff[A] = Id[A]
}
