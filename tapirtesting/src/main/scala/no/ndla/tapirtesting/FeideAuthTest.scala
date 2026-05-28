/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.tapir.auth.FeideAuth

/** Helper class for testing `FeideAuth`, using the JWK in [[TestRsaJwk]], which allows the use of
  * [[FeideAuthTestData]].
  */
class FeideAuthTest(using myNdlaProvider: MyNDLAProvider, props: BaseProps)
    extends FeideAuth(using TestJwsKeySelectorFactory(TestRsaJwk.FeideAuthKey))
