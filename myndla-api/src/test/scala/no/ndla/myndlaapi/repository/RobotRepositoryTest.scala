/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import no.ndla.common.model.NDLADate
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.myndlaapi.model.domain.{RobotConfiguration, RobotDefinition, RobotSettings, RobotStatus}
import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import java.net.Socket
import java.util.UUID
import scala.util.{Success, Try}

class RobotRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator
  override implicit lazy val DBUtil: DBUtility      = new DBUtility
  var repository: RobotRepository                   = scala.compiletime.uninitialized

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from robot_definitions;".execute()(using session)
    })
  }

  def serverIsListening: Boolean = {
    val server = props.MetaServer.unsafeGet
    val port   = props.MetaPort.unsafeGet
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  override def beforeEach(): Unit = {
    repository = new RobotRepository
    if (serverIsListening) {
      emptyTestDatabase
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  test("that inserting and retrieving a robot works as expected") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val toInsert = RobotDefinition(
      id = UUID.randomUUID(),
      feideId = "feide1",
      created = created,
      updated = created,
      shared = None,
      status = RobotStatus.PRIVATE,
      configuration = RobotConfiguration(
        title = "hei",
        version = "1.0",
        settings = RobotSettings(
          model = "gpt-4-turbo",
          name = "Mattelæreren",
          question = Some("HEi, hvordan går det?"),
          systemprompt = Some("Skriv som en luring. Svar lurt på alle spørsmålene"),
          temperature = "0.8",
        ),
      ),
    )

    val session = repository.getSession(false)
    val robot1  = repository.insertRobotDefinition(toInsert)(session).get
    robot1.configuration.title should be("hei")

    val robots = repository.getRobotsWithFeideId("feide1")(using session)
    robots.get.head should be(toInsert)
  }

  test("that updating a robot works as expected") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val toInsert = RobotDefinition(
      id = UUID.randomUUID(),
      feideId = "feide1",
      created = created,
      updated = created,
      shared = None,
      status = RobotStatus.PRIVATE,
      configuration = RobotConfiguration(
        title = "hei",
        version = "1.0",
        settings = RobotSettings(
          model = "gpt-4-turbo",
          name = "Mattelæreren",
          question = Some("HEi, hvordan går det?"),
          systemprompt = Some("Skriv som en luring. Svar lurt på alle spørsmålene"),
          temperature = "0.8",
        ),
      ),
    )

    val session = repository.getSession(false)
    val robot1  = repository.insertRobotDefinition(toInsert)(session).get
    robot1.configuration.title should be("hei")

    val toUpdate = robot1.copy(configuration = robot1.configuration.copy(title = "hei2"))
    repository.updateRobotDefinition(toUpdate)(using session).get

    val robots = repository.getRobotsWithFeideId("feide1")(using session)
    robots.get.head should be(toUpdate)
  }

}
