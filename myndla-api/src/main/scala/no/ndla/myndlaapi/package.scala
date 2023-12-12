/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object myndlaapi {
  type Eff[A] = Id[A]
}
