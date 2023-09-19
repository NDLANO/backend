/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.tapir.server.jdkhttp.Id

package object oembedproxy {
  type Eff[A] = Id[A]
}
