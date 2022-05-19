/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.articleapi.draftapi

import no.ndla.articleapi.model.api
import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.{articleapi, draftapi}
import org.eclipse.jetty.server.Server
import org.json4s.DefaultFormats
import org.testcontainers.containers.PostgreSQLContainer

class DraftApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with articleapi.TestEnvironment {
  implicit val formats: DefaultFormats = DefaultFormats
  override val ndlaClient              = new NdlaClient

  val draftApiPort: Int                 = findFreePort
  val pgc: PostgreSQLContainer[Nothing] = postgresContainer.get
  val esHost: String                    = elasticSearchHost.get
  val draftApiProperties: DraftApiProperties = new DraftApiProperties {
    override def ApplicationPort: Int = draftApiPort
    override def MetaServer: String   = pgc.getContainerIpAddress
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  val draftApi               = new draftapi.MainClass(draftApiProperties)
  val draftApiServer: Server = draftApi.startServer()
  val draftApiBaseUrl        = s"http://localhost:$draftApiPort"

  override def afterAll(): Unit = {
    super.afterAll()
    draftApiServer.stop()
  }

  private def setupAgreements() =
    (1 to 10)
      .map(id => {
        draftApi.componentRegistry.agreementRepository.insert(
          draftapi.TestData.sampleBySaDomainAgreement.copy(id = Some(id))
        )
      })

  test("should be able to fetch agreements' copyright") {
    setupAgreements()

    val expectedCopyright = api.Copyright(
      api.License(
        "CC-BY-SA-4.0",
        Some("Creative Commons Attribution-ShareAlike 4.0 International"),
        Some("https://creativecommons.org/licenses/by-sa/4.0/")
      ),
      "Origin",
      Seq.empty,
      Seq.empty,
      Seq.empty,
      None,
      None,
      None
    )

    val exampleToken =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IjQyWDkxUmEyY0M0d0xQSDZhUHloajVWQSIsImh0dHBzOi8vbmRsYS5uby91c2VyX25hbWUiOiJIdXJkaUR1cmR5IiwiaHR0cHM6Ly9uZGxhLm5vL2NsaWVudF9pZCI6IktFMjZMTTJSS05heFBsS1c5M0xMTGtPc0Vnb01Ma3BXIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDIiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTU0NzQ1ODExNiwiZXhwIjoxNTQ3NDY1MzE2LCJhenAiOiJGSzM1RkQzWUhPZWFZY1hHODBFVkNiQm1BZmlGR3ppViIsInNjb3BlIjoibGlzdGluZzp3cml0ZSBkcmFmdHM6YWRtaW4gbGVhcm5pbmdwYXRoOmFkbWluIGRyYWZ0czpzZXRfdG9fcHVibGlzaCBhdWRpbzp3cml0ZSBpbWFnZXM6d3JpdGUgY29uY2VwdDp3cml0ZSBkcmFmdHM6d3JpdGUgdGF4b25vbXk6d3JpdGUgYXJ0aWNsZXM6d3JpdGUgIn0K.hgk3TpqXpCerofnVaE17ZH7r4Cr3ehaiOl95hsipQrL9OuJvzDx1Y7-DrGfk6Y3-cE4qwScEahpYVf_aOIuXPNNKfbtRYPV3H84T1B02j1olhlLzEbJ-BGyvN2J6CpVy2PHfSUpTVjOMB7q4IDti2NUlhYSXCY4_ZAZhN20wXqID71ZjWqwNRJh1xUXfBQFOkFkCRxgYgEUA_oBrVBgx66iXhIrTDVcGPNyLRui40LMDnQFTU7lel-c1BdK393MHq9lQq5Yg3x5tFJ3k3HA682_UCiN8HzNvLaE5bUyskXPT-Qy57uWNuO0bpUkiRxB2rYwUR5OGYSCWz9fSbIKptQ"

    AuthUser.setHeader(s"Bearer $exampleToken")
    val draftApiClient = new DraftApiClient(draftApiBaseUrl)
    val copyright      = draftApiClient.getAgreementCopyright(1)
    copyright.get should be(expectedCopyright)
  }

}
