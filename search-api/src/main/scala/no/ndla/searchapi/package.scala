/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object searchapi {
  type Eff[A] = Id[A]
}
