/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.shared.Identity

package object conceptapi {
  type Eff[A] = Identity[A]
}
