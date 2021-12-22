/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.model.api
import no.ndla.network.AuthUser
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

class DraftApiClientTest extends UnitSuite with TestEnvironment {
  implicit val formats: DefaultFormats = DefaultFormats
  override val ndlaClient = new NdlaClient

  // Pact CDC imports
  import com.itv.scalapact.ScalaPactForger._
  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

  test("should be able to fetch agreements' copyright") {
    val expectedCopyright = api.Copyright(
      api.License("CC-BY-SA-4.0",
                  Some("Creative Commons Attribution-ShareAlike 4.0 International"),
                  Some("https://creativecommons.org/licenses/by-sa/4.0/")),
      "Origin",
      Seq.empty,
      Seq.empty,
      Seq.empty,
      None,
      None,
      None
    )
    val body = write(Agreement(1, expectedCopyright))

    val exampleToken =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IjQyWDkxUmEyY0M0d0xQSDZhUHloajVWQSIsImh0dHBzOi8vbmRsYS5uby91c2VyX25hbWUiOiJIdXJkaUR1cmR5IiwiaHR0cHM6Ly9uZGxhLm5vL2NsaWVudF9pZCI6IktFMjZMTTJSS05heFBsS1c5M0xMTGtPc0Vnb01Ma3BXIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDIiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTU0NzQ1ODExNiwiZXhwIjoxNTQ3NDY1MzE2LCJhenAiOiJGSzM1RkQzWUhPZWFZY1hHODBFVkNiQm1BZmlGR3ppViIsInNjb3BlIjoibGlzdGluZzp3cml0ZSBkcmFmdHM6YWRtaW4gbGVhcm5pbmdwYXRoOmFkbWluIGRyYWZ0czpzZXRfdG9fcHVibGlzaCBhdWRpbzp3cml0ZSBpbWFnZXM6d3JpdGUgY29uY2VwdDp3cml0ZSBkcmFmdHM6d3JpdGUgdGF4b25vbXk6d3JpdGUgYXJ0aWNsZXM6d3JpdGUgIn0K.hgk3TpqXpCerofnVaE17ZH7r4Cr3ehaiOl95hsipQrL9OuJvzDx1Y7-DrGfk6Y3-cE4qwScEahpYVf_aOIuXPNNKfbtRYPV3H84T1B02j1olhlLzEbJ-BGyvN2J6CpVy2PHfSUpTVjOMB7q4IDti2NUlhYSXCY4_ZAZhN20wXqID71ZjWqwNRJh1xUXfBQFOkFkCRxgYgEUA_oBrVBgx66iXhIrTDVcGPNyLRui40LMDnQFTU7lel-c1BdK393MHq9lQq5Yg3x5tFJ3k3HA682_UCiN8HzNvLaE5bUyskXPT-Qy57uWNuO0bpUkiRxB2rYwUR5OGYSCWz9fSbIKptQ"

    forgePact
      .between("article-api")
      .and("draft-api")
      .addInteraction(
        interaction
          .description("Fetching agreement with id 1")
          .given("agreements")
          .uponReceiving(
            GET,
            "/draft-api/v1/agreements/1",
            None,
            Map("Authorization" -> s"Bearer $exampleToken"),
            None,
            None
          )
          .willRespondWith(
            status = 200,
            body = body
          )
      )
      .runConsumerTest { mockConfig =>
        AuthUser.setHeader(s"Bearer $exampleToken")
        val draftApiClient = new DraftApiClient(mockConfig.baseUrl)
        val copyright = draftApiClient.getAgreementCopyright(1)
        copyright.get should be(expectedCopyright)
      }
  }

}
