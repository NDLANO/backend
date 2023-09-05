/*
 * Part of NDLA audio-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object audioapi {
  type Eff[A] = Id[A]
}
