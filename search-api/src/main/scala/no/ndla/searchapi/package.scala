/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.shared.Identity

package object searchapi {
  type Eff[A] = Identity[A]
}
