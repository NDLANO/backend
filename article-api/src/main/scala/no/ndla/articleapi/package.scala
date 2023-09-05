/*
 * Part of NDLA article-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object articleapi {
  type Eff[A] = Id[A]
}
