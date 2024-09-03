/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.shared.Identity

package object oembedproxy {
  type Eff[A] = Identity[A]
}
