/*
 * Part of NDLA article-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla

import sttp.shared.Identity

package object articleapi {
  type Eff[A] = Identity[A]
}
