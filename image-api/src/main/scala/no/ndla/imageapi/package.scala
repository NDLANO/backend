/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla

import sttp.shared.Identity

package object imageapi {
  type Eff[A] = Identity[A]
}
