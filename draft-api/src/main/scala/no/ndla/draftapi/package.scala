/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla

import sttp.shared.Identity

package object draftapi {
  type Eff[A] = Identity[A]
}
