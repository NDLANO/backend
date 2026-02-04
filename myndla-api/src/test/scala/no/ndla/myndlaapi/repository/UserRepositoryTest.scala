/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.{MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import scalikejdbc.*
import org.mockito.Mockito.when

import java.net.Socket
import scala.util.{Success, Try}

class UserRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator
  override implicit lazy val DBUtil: DBUtility      = new DBUtility
  var repository: UserRepository                    = scala.compiletime.uninitialized

  val feideId                   = "feide1"
  val baseLastUpdated: NDLADate = NDLADate.of(2024, 1, 1, 12, 0, 0)

  def emptyTestDatabase: Boolean = {
    DBUtil.writeSession(implicit session => {
      sql"delete from my_ndla_users;".execute()(using session)
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
    repository = new UserRepository
    when(clock.now()).thenReturn(NDLADate.now())
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

  def userDocument(
      displayName: String,
      username: String,
      role: UserRole,
      favoriteSubjects: Seq[String] = Seq.empty,
      arenaEnabled: Boolean = false,
  ): MyNDLAUserDocument = MyNDLAUserDocument(
    favoriteSubjects = favoriteSubjects,
    userRole = role,
    lastUpdated = baseLastUpdated,
    organization = "NDLA",
    groups = Seq.empty,
    username = username,
    displayName = displayName,
    email = s"$username@ndla.no",
    arenaEnabled = arenaEnabled,
  )

  def insertUser(feideId: String, document: MyNDLAUserDocument)(implicit session: DBSession): MyNDLAUser = {
    repository.reserveFeideIdIfNotExists(feideId)
    repository.insertUser(feideId, document).get
  }

  test("that lastSeen gets updated correctly") {
    implicit val session: DBSession = DBUtil.autoSession

    val inserted =
      insertUser(feideId, userDocument(displayName = "Alice", username = "alice", role = UserRole.EMPLOYEE))

    val updatedLastSeen = NDLADate.of(2024, 2, 1, 10, 15, 0).withNano(0)
    val updatedUser     = inserted.copy(displayName = "Alice Updated", lastSeen = updatedLastSeen)

    repository.updateUser(feideId, updatedUser)(using session).get

    val fetched = repository.userWithFeideId(feideId)(using session).get.get
    fetched.lastSeen should be(updatedLastSeen)
    fetched.displayName should be("Alice Updated")
  }

  test("that reserveFeideIdIfNotExists reports existing rows correctly") {
    implicit val session: DBSession = DBUtil.autoSession

    repository.reserveFeideIdIfNotExists("feide-reserve").get should be(false)
    repository.reserveFeideIdIfNotExists("feide-reserve").get should be(true)
  }

  test("that getUsersPaginated respects teacher filter and query") {
    implicit val session: DBSession = DBUtil.autoSession

    insertUser("feide-teacher-1", userDocument("Alice Teacher", "alice", UserRole.EMPLOYEE))
    insertUser("feide-student-1", userDocument("Bob Student", "bob", UserRole.STUDENT))
    insertUser("feide-teacher-2", userDocument("Carol Teacher", "carol", UserRole.EMPLOYEE))

    val (teacherCount, teachers) = repository
      .getUsersPaginated(offset = 0, limit = 10, filterTeachers = true, query = Some("ali"))
      .get
    teacherCount should be(1)
    teachers.map(_.username) should be(List("alice"))

    val (queryCount, queryUsers) = repository
      .getUsersPaginated(offset = 0, limit = 10, filterTeachers = false, query = Some("bob"))
      .get
    queryCount should be(1)
    queryUsers.map(_.username) should be(List("bob"))
  }

  test("that usersGrouped and numberOfFavouritedSubjects return expected counts") {
    implicit val session: DBSession = DBUtil.autoSession

    insertUser(
      "feide-employee-1",
      userDocument("Teacher One", "teacher1", UserRole.EMPLOYEE, favoriteSubjects = Seq("math", "science")),
    )
    insertUser(
      "feide-student-1",
      userDocument("Student One", "student1", UserRole.STUDENT, favoriteSubjects = Seq("english")),
    )
    insertUser("feide-employee-2", userDocument("Teacher Two", "teacher2", UserRole.EMPLOYEE))

    repository.usersGrouped()(using session).get should be(Map(UserRole.EMPLOYEE -> 2L, UserRole.STUDENT -> 1L))
    repository.numberOfFavouritedSubjects()(using session).get should be(Some(3L))
  }

  test("that fetching users older than a given date works") {
    DBUtil.writeSession { implicit session =>
      val now      = NDLADate.now()
      val veryOld  = now.minusDays(300)
      val notSoOld = now.minusDays(10)
      when(clock.now()).thenReturn(veryOld)
      val oldUser = insertUser("feide-old-user", userDocument("Old User", "olduser", UserRole.STUDENT))
      when(clock.now()).thenReturn(notSoOld)
      val newerUser = insertUser("feide-newer-user", userDocument("Newer User", "neweruser", UserRole.STUDENT))

      val fetchedOldUsers = repository.findUsersOlderThan(now.minusDays(200))(using session).get
      fetchedOldUsers.head.feideId should be(oldUser.feideId)
      fetchedOldUsers.head.lastSeen should be(veryOld)
      fetchedOldUsers.size should be(1)

      val fetchedOldUsers2 = repository.findUsersOlderThan(now.minusDays(5))(using session).get
      fetchedOldUsers2.head.feideId should be(oldUser.feideId)
      fetchedOldUsers2.head.lastSeen should be(veryOld)
      fetchedOldUsers2(1).feideId should be(newerUser.feideId)
      fetchedOldUsers2(1).lastSeen should be(notSoOld)
      fetchedOldUsers2.size should be(2)
    }
  }
}
