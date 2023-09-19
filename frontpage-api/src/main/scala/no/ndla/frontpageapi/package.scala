/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object frontpageapi {
  type Eff[A] = Id[A]
}
