/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.articleapi.draftapi

import no.ndla.common.model.NDLADate
import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.{articleapi, draftapi}
import org.eclipse.jetty.server.Server
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.JavaTimeSerializers
import org.testcontainers.containers.PostgreSQLContainer

class DraftApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with articleapi.TestEnvironment {
  implicit val formats: Formats = DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
  override val ndlaClient       = new NdlaClient

  val draftApiPort: Int           = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val draftApiProperties: DraftApiProperties = new DraftApiProperties {
    override def ApplicationPort: Int = draftApiPort
    override def MetaServer: String   = pgc.getHost
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

}
