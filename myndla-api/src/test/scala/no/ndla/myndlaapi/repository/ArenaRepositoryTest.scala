/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.{MyNDLAUserDocument, UserRole}
import no.ndla.myndlaapi.model.arena.domain.InsertCategory
import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite

class ArenaRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true, schemaName = "myndlaapi_test")
    with UnitSuite
    with TestEnvironment {
  override val dataSource: HikariDataSource = testDataSource.get
  override val migrator                     = new DBMigrator

  override val arenaRepository: ArenaRepository = new ArenaRepository
  override val userRepository: UserRepository   = new UserRepository

  def emptyTestDatabase(): Unit = {
    arenaRepository.withSession(implicit session => {
      arenaRepository.deleteAllFollows.get
      arenaRepository.deleteAllPosts.get
      arenaRepository.deleteAllTopics.get
      arenaRepository.deleteAllCategories.get
      arenaRepository.resetSequences.get
      userRepository.deleteAllUsers.get
      userRepository.resetSequences.get
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    DataSource.connectToDatabase()
    migrator.migrate()
  }

  override def beforeEach(): Unit = {
    DataSource.connectToDatabase()
    emptyTestDatabase()
  }

  test("That inserting and retrieving categories and topics works as expected") {
    val now = NDLADate.now()
    val user = MyNDLAUserDocument(
      favoriteSubjects = List(),
      userRole = UserRole.EMPLOYEE,
      lastUpdated = now,
      organization = "org",
      groups = List(),
      username = "test",
      displayName = "Test Testesen",
      email = "example@example.com",
      arenaEnabled = true,
      arenaGroups = List(),
      arenaAccepted = true
    )
    val feideId = "feideId1"

    arenaRepository.withSession { session =>
      userRepository.reserveFeideIdIfNotExists(feideId)(session).get
      val user1 = userRepository.insertUser(feideId, user)(session).get

      val cat1 = arenaRepository
        .insertCategory(
          InsertCategory(
            title = "Category 1",
            description = "Category 1 description",
            visible = true,
            parentCategoryId = None
          )
        )(session)
        .get

      val top1 = arenaRepository
        .insertTopic(
          categoryId = cat1.id,
          title = "Topic 1",
          ownerId = user1.id,
          created = now,
          updated = now,
          locked = false,
          pinned = false
        )(session)
        .get

      val topic = arenaRepository.getTopic(top1.id, user1)(session).get
      topic.get.topic.title should be("Topic 1")

      val paginated = arenaRepository.getTopicsPaginated(0, 10, user1)(session).get
      paginated._1.head.topic.title should be("Topic 1")
    }
  }

  test("That post count is updated when post is inserted") {
    val now = NDLADate.now()
    val user = MyNDLAUserDocument(
      favoriteSubjects = List(),
      userRole = UserRole.EMPLOYEE,
      lastUpdated = now,
      organization = "org",
      groups = List(),
      username = "test",
      displayName = "Test Testesen",
      email = "example@example.com",
      arenaEnabled = true,
      arenaGroups = List(),
      arenaAccepted = true
    )
    val feideId = "feideId1"

    arenaRepository.withSession { session =>
      userRepository.reserveFeideIdIfNotExists(feideId)(session).get
      val user1 = userRepository.insertUser(feideId, user)(session).get

      val cat1 = arenaRepository
        .insertCategory(
          InsertCategory(
            title = "Category 1",
            description = "Category 1 description",
            visible = true,
            parentCategoryId = None
          )
        )(session)
        .get

      val top1 = arenaRepository
        .insertTopic(
          categoryId = cat1.id,
          title = "Topic 1",
          ownerId = user1.id,
          created = now,
          updated = now,
          locked = false,
          pinned = false
        )(session)
        .get

      val topic = arenaRepository.getTopic(top1.id, user1)(session).get
      topic.get.postCount should be(0)

      arenaRepository
        .postPost(
          topicId = top1.id,
          content = "Post 1",
          ownerId = user1.id,
          created = now,
          updated = now,
          toPostId = None
        )(session)
        .get

      val topicAfterPost = arenaRepository.getTopic(top1.id, user1)(session).get
      topicAfterPost.get.postCount should be(1)

    }
  }

  test("That upvote count is updated when main post in topic is upvoted") {
    val now = NDLADate.now()
    val user = MyNDLAUserDocument(
      favoriteSubjects = List(),
      userRole = UserRole.EMPLOYEE,
      lastUpdated = now,
      organization = "org",
      groups = List(),
      username = "test",
      displayName = "Test Testesen",
      email = "example@example.com",
      arenaEnabled = true,
      arenaGroups = List(),
      arenaAccepted = true
    )
    val feideId  = "feideId1"
    val feideId2 = "feideId2"

    arenaRepository.withSession { session =>
      userRepository.reserveFeideIdIfNotExists(feideId)(session).get
      userRepository.reserveFeideIdIfNotExists(feideId2)(session).get
      val user1 =
        userRepository.insertUser(feideId, user.copy(username = "test1", email = "example1@example.com"))(session).get
      val user2 =
        userRepository.insertUser(feideId2, user.copy(username = "test2", email = "example2@example.com"))(session).get

      val cat1 = arenaRepository
        .insertCategory(
          InsertCategory(
            title = "Category 1",
            description = "Category 1 description",
            visible = true,
            parentCategoryId = None
          )
        )(session)
        .get

      val top1 = arenaRepository
        .insertTopic(
          categoryId = cat1.id,
          title = "Topic 1",
          ownerId = user1.id,
          created = now,
          updated = now,
          locked = false,
          pinned = false
        )(session)
        .get

      val topic = arenaRepository.getTopic(top1.id, user1)(session).get
      topic.get.postCount should be(0)

      val mainPost = arenaRepository
        .postPost(
          topicId = top1.id,
          content = "Post 1",
          ownerId = user2.id,
          created = now,
          updated = now,
          toPostId = None
        )(session)
        .get

      val secondPost = arenaRepository
        .postPost(
          topicId = top1.id,
          content = "Post 1",
          ownerId = user2.id,
          created = now,
          updated = now,
          toPostId = None
        )(session)
        .get

      val topicAfterPost = arenaRepository.getTopic(top1.id, user1)(session).get
      topicAfterPost.get.postCount should be(2)
      topicAfterPost.get.voteCount should be(0)
      val categoryTopicsAfterPost = arenaRepository.getTopicsForCategory(cat1.id, 0, 10, user1)(session).get
      categoryTopicsAfterPost.head.voteCount should be(0)

      arenaRepository.upvotePost(mainPost.id, user2.id)(session).get
      val topicAfterMainUpvote = arenaRepository.getTopic(top1.id, user1)(session).get
      topicAfterMainUpvote.get.voteCount should be(1)
      val categoryTopicsAfterMainUpvote = arenaRepository.getTopicsForCategory(cat1.id, 0, 10, user1)(session).get
      categoryTopicsAfterMainUpvote.head.voteCount should be(1)

      // Only main post of topic should count towards vote count
      // this is how it is in nodebb
      arenaRepository.upvotePost(secondPost.id, user2.id)(session).get
      val topicAfterOtherUpvote = arenaRepository.getTopic(top1.id, user1)(session).get
      topicAfterOtherUpvote.get.voteCount should be(1)
      val categoryTopicsAfterOtherUpvote = arenaRepository.getTopicsForCategory(cat1.id, 0, 10, user1)(session).get
      categoryTopicsAfterOtherUpvote.head.voteCount should be(1)
    }
  }
}
