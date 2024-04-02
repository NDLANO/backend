/*
 * Part of NDLA network
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

class AuthUserTest extends UnitSuite {

  test("getAuth0HostForEnv should return correct hostnames for environments") {
    AuthUser.getAuth0HostForEnv("test") should equal("ndla-test.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("spoletest") should equal("ndla-test.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("brukertest") should equal("ndla-test.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("local") should equal("ndla-test.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("staging") should equal("ndla-staging.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("prod") should equal("ndla.eu.auth0.com")
    AuthUser.getAuth0HostForEnv("ff") should equal("ndla.eu.auth0.com")
  }

  test("getAuth0HostForEnv should return the test hostname for any undefined environments") {
    AuthUser.getAuth0HostForEnv("tullball") should equal("ndla-test.eu.auth0.com")
  }
}
