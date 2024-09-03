/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla

import sttp.shared.Identity

package object learningpathapi {
  type Eff[A] = Identity[A]
}
