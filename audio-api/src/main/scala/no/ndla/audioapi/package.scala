/*
 * Part of NDLA audio-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.shared.Identity

package object audioapi {
  type Eff[A] = Identity[A]
}
