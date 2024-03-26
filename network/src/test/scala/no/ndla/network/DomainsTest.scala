/*
 * Part of NDLA network
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

class DomainsTest extends UnitSuite {

  test("That local env returns localhost") {
    Domains.get("local") should equal("http://api-gateway.ndla-local")
  }

  test("That prod env returns prod") {
    Domains.get("prod") should equal("https://api.ndla.no")
  }

  test("That ant other env returns any other env") {
    Domains.get("anyotherenv") should equal("https://api.anyotherenv.ndla.no")
  }
}
