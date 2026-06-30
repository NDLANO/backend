/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import no.ndla.common.configuration.BaseProps
import no.ndla.network.tapir.ErrorHelpers
import no.ndla.network.tapir.auth.NdlaAuth

/** Helper class for testing `NdlaAuth`, using the JWK in [[TestRsaJwk]], which allows the use of
  * [[NdlaAuthTestTokens]].
  */
class NdlaAuthTest(using errorHelpers: ErrorHelpers, props: BaseProps)
    extends NdlaAuth(using TestJwsKeySelectorFactory(TestRsaJwk.NdlaAuthKey))
