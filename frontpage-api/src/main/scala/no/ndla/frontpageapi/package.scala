/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla

import sttp.shared.Identity

package object frontpageapi {
  type Eff[A] = Identity[A]
}
