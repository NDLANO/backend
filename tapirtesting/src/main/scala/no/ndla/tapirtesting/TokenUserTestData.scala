/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import no.ndla.common.auth.Permission
import no.ndla.network.tapir.auth.TokenUser

object TokenUserTestData {
  val PublicUser: TokenUser = TokenUser("public", Set.empty, None)
  val SystemUser: TokenUser = TokenUser("system", Permission.values.toSet, Some(NdlaAuthTestTokens.AllPermissions))
}
