/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.{ArenaGroup, MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.arena.api
import no.ndla.myndlaapi.*
import no.ndla.myndlaapi.model.api.ArenaUserDTO
import no.ndla.myndlaapi.model.arena.api.{NewCategoryDTO, PaginatedNewPostNotificationsDTO, PaginatedPostsDTO}
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, spy, when, withSettings}
import org.mockito.quality.Strictness
import org.testcontainers.containers.PostgreSQLContainer
import sttp.client3.Response
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Success

class ArenaTest
    extends IntegrationSuite(
      EnableElasticsearchContainer = false,
      EnablePostgresContainer = true,
      EnableRedisContainer = true
    )
    with UnitSuite {

  val myndlaApiPort: Int          = findFreePort
  val pgc: PostgreSQLContainer[?] = postgresContainer.get
  val redisPort: Int              = redisContainer.get.port
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = redisPort
  }

  val someDate: NDLADate = NDLADate.of(2017, 1, 1, 1, 59)
  val feideId            = "feide"

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override lazy val clock: SystemClock = mock[SystemClock](withSettings.strictness(Strictness.LENIENT))
      override lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override lazy val userRepository: UserRepository     = spy(new UserRepository)
      override lazy val userService: UserService           = spy(new UserService)
      override lazy val configService: ConfigService       = spy(new ConfigService)

      when(feideApiClient.getFeideID(any)).thenReturn(Success("q"))
      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any))
        .thenReturn(
          Success(FeideExtendedUserInfo("", Seq("employee"), None, "email@ndla.no", Some(Seq("email@ndla.no"))))
        )
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(clock.now()).thenReturn(someDate)
    }
  }

  val testClock: myndlaApi.componentRegistry.SystemClock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl: String  = s"http://localhost:$myndlaApiPort"
  val myndlaApiArenaUrl: String = s"$myndlaApiBaseUrl/myndla-api/v1/arena"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future { myndlaApi.run() }: Unit
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(myndlaApi.componentRegistry.userService)
    reset(myndlaApi.componentRegistry.userRepository)

    myndlaApi.componentRegistry.DBUtil.withSession(implicit session => {
      myndlaApi.componentRegistry.arenaRepository.deleteAllFollows.get
      myndlaApi.componentRegistry.arenaRepository.deleteAllPosts.get
      myndlaApi.componentRegistry.arenaRepository.deleteAllTopics.get
      myndlaApi.componentRegistry.arenaRepository.deleteAllCategories.get
      myndlaApi.componentRegistry.arenaRepository.resetSequences.get
      myndlaApi.componentRegistry.userRepository.deleteAllUsers.get
      myndlaApi.componentRegistry.userRepository.resetSequences.get
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val testUser: MyNDLAUser = MyNDLAUser(
    id = 1,
    feideId = feideId,
    favoriteSubjects = Seq.empty,
    userRole = UserRole.EMPLOYEE,
    lastUpdated = TestData.today,
    organization = "yap",
    groups = Seq.empty,
    username = "username",
    displayName = "displayName",
    email = "some@example.com",
    arenaEnabled = true,
    arenaAccepted = true,
    arenaGroups = List.empty,
    shareNameAccepted = false
  )

  val testAdmin: MyNDLAUser = MyNDLAUser(
    id = 2,
    feideId = feideId,
    favoriteSubjects = Seq.empty,
    userRole = UserRole.EMPLOYEE,
    lastUpdated = TestData.today,
    organization = "yap",
    groups = Seq.empty,
    username = "username",
    displayName = "displayName",
    email = "some@example.com",
    arenaEnabled = true,
    arenaAccepted = true,
    arenaGroups = List(ArenaGroup.ADMIN),
    shareNameAccepted = false
  )

  def createCategory(
      title: String,
      description: String,
      shouldSucceed: Boolean = true,
      parentCategoryId: Option[Long] = None
  ): Response[String] = {
    val newCategory =
      api.NewCategoryDTO(title = title, description = description, visible = true, parentCategoryId = parentCategoryId)
    val inBody = newCategory.asJson.noSpaces
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/categories")
        .body(inBody)
        .header("Content-type", "application/json")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    res
  }

  def createTopic(
      title: String,
      content: String,
      categoryId: Long,
      shouldSucceed: Boolean = true,
      token: String = "asd"
  ): Response[String] = {
    val newTopic =
      api.NewTopicDTO(
        title = title,
        initialPost = api.NewPostDTO(content = content, None),
        isLocked = Some(false),
        isPinned = Some(false)
      )
    val inBody = newTopic.asJson.noSpaces
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/categories/$categoryId/topics")
        .body(inBody)
        .header("Content-type", "application/json")
        .header("FeideAuthorization", s"Bearer $token")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    res
  }

  def createPost(
      content: String,
      topicId: Long,
      shouldSucceed: Boolean = true,
      token: String = "asd",
      toPostId: Option[Long] = None
  ): Response[String] = {
    val newPost = api.NewPostDTO(content = content, toPostId = toPostId)
    val inBody  = newPost.asJson.noSpaces
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/topics/$topicId/posts")
        .body(inBody)
        .header("Content-type", "application/json")
        .header("FeideAuthorization", s"Bearer $token")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    res
  }

  test("that creating and fetching all categories works") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(myndlaApi.componentRegistry.userService.getArenaEnabledUser(any)).thenReturn(Success(testAdmin))

    createCategory("title", "description")

    val fetchCategoriesResponse = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/categories")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )

    val categories = io.circe.parser.parse(fetchCategoriesResponse.body).flatMap(_.as[List[api.CategoryDTO]]).toTry.get
    categories.size should be(1)
    categories.head should be(
      api.CategoryDTO(
        1,
        "title",
        "description",
        0,
        0,
        isFollowing = false,
        visible = true,
        rank = 1,
        parentCategoryId = None,
        categoryCount = 0,
        subcategories = List.empty,
        breadcrumbs = List(api.CategoryBreadcrumbDTO(1, "title"))
      )
    )
  }

  test("that creating a category with a bunch of topics and posts works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val top1 = createTopic("title1", "description1", categoryId)
    createTopic("title2", "description2", categoryId)
    createTopic("title3", "description3", categoryId)

    val top1T  = io.circe.parser.parse(top1.body).flatMap(_.as[api.TopicDTO]).toTry
    val top1Id = top1T.get.id

    createPost("post1", top1Id)
    createPost("post2", top1Id)
    createPost("post3", top1Id)
    createPost("post4", top1Id)

    val expectedCategoryResult = api.CategoryWithTopicsDTO(
      id = 1,
      title = "title",
      description = "description",
      topicCount = 3,
      postCount = 7,
      topicPage = 1,
      topicPageSize = 10,
      topics = List(
        api.TopicDTO(
          id = 1,
          title = "title1",
          postCount = 5,
          created = someDate,
          updated = someDate,
          categoryId = 1,
          isFollowing = true,
          isLocked = false,
          isPinned = false,
          voteCount = 0
        ),
        api.TopicDTO(
          id = 2,
          title = "title2",
          postCount = 1,
          created = someDate,
          updated = someDate,
          categoryId = 1,
          isFollowing = true,
          isLocked = false,
          isPinned = false,
          voteCount = 0
        ),
        api.TopicDTO(
          id = 3,
          title = "title3",
          postCount = 1,
          created = someDate,
          updated = someDate,
          categoryId = 1,
          isFollowing = true,
          isLocked = false,
          isPinned = false,
          voteCount = 0
        )
      ),
      isFollowing = false,
      visible = true,
      rank = 1,
      categoryCount = 0,
      subcategories = List.empty,
      parentCategoryId = None,
      breadcrumbs = List(api.CategoryBreadcrumbDTO(1, "title"))
    )

    val categoryResp = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/categories/1")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )

    categoryResp.code.code should be(200)

    val resultTry = io.circe.parser.parse(categoryResp.body).flatMap(_.as[api.CategoryWithTopicsDTO]).toTry
    resultTry should be(Success(expectedCategoryResult))

    val expectedTopic1Result = api.TopicWithPostsDTO(
      id = 1,
      title = "title1",
      postCount = 5,
      posts = api.PaginatedPostsDTO(
        totalCount = 5,
        page = 1,
        pageSize = 10,
        items = List(
          api.PostDTO(
            id = 1,
            content = "description1",
            created = someDate,
            updated = someDate,
            owner = Some(
              ArenaUserDTO(
                id = 1,
                displayName = "",
                username = "email@ndla.no",
                location = "zxc",
                groups = List(ArenaGroup.ADMIN)
              )
            ),
            flags = Some(List()),
            topicId = 1,
            replies = List.empty,
            upvotes = 0,
            upvoted = false
          ),
          api.PostDTO(
            id = 4,
            content = "post1",
            created = someDate,
            updated = someDate,
            owner = Some(
              model.api.ArenaUserDTO(
                id = 1,
                displayName = "",
                username = "email@ndla.no",
                location = "zxc",
                groups = List(ArenaGroup.ADMIN)
              )
            ),
            flags = Some(List()),
            topicId = 1,
            replies = List.empty,
            upvotes = 0,
            upvoted = false
          ),
          api.PostDTO(
            id = 5,
            content = "post2",
            created = someDate,
            updated = someDate,
            owner = Some(
              model.api.ArenaUserDTO(
                id = 1,
                displayName = "",
                username = "email@ndla.no",
                location = "zxc",
                groups = List(ArenaGroup.ADMIN)
              )
            ),
            flags = Some(List()),
            topicId = 1,
            replies = List.empty,
            upvotes = 0,
            upvoted = false
          ),
          api.PostDTO(
            id = 6,
            content = "post3",
            created = someDate,
            updated = someDate,
            owner = Some(
              model.api.ArenaUserDTO(
                id = 1,
                displayName = "",
                username = "email@ndla.no",
                location = "zxc",
                groups = List(ArenaGroup.ADMIN)
              )
            ),
            flags = Some(List()),
            topicId = 1,
            replies = List.empty,
            upvotes = 0,
            upvoted = false
          ),
          api.PostDTO(
            id = 7,
            content = "post4",
            created = someDate,
            updated = someDate,
            owner = Some(
              model.api.ArenaUserDTO(
                id = 1,
                displayName = "",
                username = "email@ndla.no",
                location = "zxc",
                groups = List(ArenaGroup.ADMIN)
              )
            ),
            flags = Some(List()),
            topicId = 1,
            replies = List.empty,
            upvotes = 0,
            upvoted = false
          )
        )
      ),
      created = someDate,
      updated = someDate,
      categoryId = 1,
      isFollowing = true,
      isLocked = false,
      isPinned = false,
      voteCount = 0
    )

    val topic1Resp = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/topics/1")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )
    val topic1ResultTry = io.circe.parser.parse(topic1Resp.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry
    topic1ResultTry should be(Success(expectedTopic1Result))
    topic1Resp.code.code should be(200)
  }

  test("that fetching a post in a topic context returns correct page") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val top1 = createTopic("title1", "description1", categoryId)

    val top1T  = io.circe.parser.parse(top1.body).flatMap(_.as[api.TopicDTO]).toTry
    val top1Id = top1T.get.id

    createPost("post1", top1Id)
    createPost("post2", top1Id)
    createPost("post3", top1Id)
    createPost("post4", top1Id)
    createPost("post5", top1Id)
    createPost("post6", top1Id)
    createPost("post7", top1Id)
    createPost("post8", top1Id)
    createPost("post9", top1Id)
    createPost("post10", top1Id)
    createPost("post11", top1Id)
    createPost("post12", top1Id)
    createPost("post13", top1Id)
    createPost("post14", top1Id)
    createPost("post15", top1Id)
    createPost("post16", top1Id)
    createPost("post17", top1Id)
    createPost("post18", top1Id)
    createPost("post19", top1Id)
    createPost("post20", top1Id)
    createPost("post21", top1Id)

    def post(num: Long): api.PostDTO = {
      api.PostDTO(
        id = num + 1,
        content = s"post$num",
        created = someDate,
        updated = someDate,
        owner = Some(
          model.api.ArenaUserDTO(
            id = 1,
            displayName = "",
            username = "email@ndla.no",
            location = "zxc",
            groups = List(ArenaGroup.ADMIN)
          )
        ),
        flags = Some(List()),
        topicId = 1,
        replies = List.empty,
        upvotes = 0,
        upvoted = false
      )
    }

    {
      val expectedTopic1Result = api.TopicWithPostsDTO(
        id = 1,
        title = "title1",
        postCount = 22,
        posts = api.PaginatedPostsDTO(
          totalCount = 22,
          page = 2,
          pageSize = 10,
          items = List(
            post(10),
            post(11),
            post(12),
            post(13),
            post(14),
            post(15),
            post(16),
            post(17),
            post(18),
            post(19)
          )
        ),
        created = someDate,
        updated = someDate,
        categoryId = 1,
        isFollowing = true,
        isLocked = false,
        isPinned = false,
        voteCount = 0
      )

      val topic1Resp = simpleHttpClient.send(
        quickRequest
          .get(uri"$myndlaApiArenaUrl/posts/14/topic")
          .header("FeideAuthorization", s"Bearer asd")
          .readTimeout(10.seconds)
      )
      val topic1ResultTry = io.circe.parser.parse(topic1Resp.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry
      topic1ResultTry should be(Success(expectedTopic1Result))
      topic1Resp.code.code should be(200)
    }

    {
      val expectedTopic1Result = api.TopicWithPostsDTO(
        id = 1,
        title = "title1",
        postCount = 22,
        posts = api.PaginatedPostsDTO(
          totalCount = 22,
          page = 5,
          pageSize = 3,
          items = List(
            post(12),
            post(13),
            post(14)
          )
        ),
        created = someDate,
        updated = someDate,
        categoryId = 1,
        isFollowing = true,
        isLocked = false,
        isPinned = false,
        voteCount = 0
      )

      val topic1Resp = simpleHttpClient.send(
        quickRequest
          .get(uri"$myndlaApiArenaUrl/posts/13/topic?page-size=3")
          .header("FeideAuthorization", s"Bearer asd")
          .readTimeout(10.seconds)
      )
      val topic1ResultTry = io.circe.parser.parse(topic1Resp.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry
      topic1ResultTry should be(Success(expectedTopic1Result))
      topic1Resp.code.code should be(200)
    }
  }

  test("that fetching posting in topic generates notification for follower") {
    val user1Token = "user1"
    val user1Id    = "user1Id"
    val user2Token = "user2"
    val user2Id    = "user2Id"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(user1Token)))).thenReturn(Success(user1Id))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(user2Token)))).thenReturn(Success(user2Id))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    // User 1 creates topic which should result in following own topic
    val createTopicRes = createTopic("title1", "description1", categoryId, token = user1Token)
    val topicIdT       = io.circe.parser.parse(createTopicRes.body).flatMap(_.as[api.TopicDTO]).toTry
    val topicId        = topicIdT.get.id

    // User 2 posts to user1s followed topic
    createPost("noe innhold i topicen", topicId, token = user2Token)

    { // User 1 should have a notification
      val notificationsResp = simpleHttpClient.send(
        quickRequest
          .get(uri"$myndlaApiArenaUrl/notifications")
          .header("FeideAuthorization", s"Bearer $user1Token")
          .readTimeout(10.seconds)
      )
      val notificationT =
        io.circe.parser.parse(notificationsResp.body).flatMap(_.as[PaginatedNewPostNotificationsDTO]).toTry
      notificationT.get.items.size should be(1)
      val notif = notificationT.get.items.head
      notif.topicId should be(topicId)
      notif.isRead should be(false)
    }

    { // User 1 fetches topic
      val topic1Resp = simpleHttpClient.send(
        quickRequest
          .get(uri"$myndlaApiArenaUrl/topics/$topicId")
          .header("FeideAuthorization", s"Bearer $user1Token")
          .readTimeout(10.seconds)
      )
      topic1Resp.code.code should be(200)
    }

    { // User 1 should have read notification
      val notificationsResp = simpleHttpClient.send(
        quickRequest
          .get(uri"$myndlaApiArenaUrl/notifications")
          .header("FeideAuthorization", s"Bearer $user1Token")
          .readTimeout(10.seconds)
      )
      val notificationT =
        io.circe.parser.parse(notificationsResp.body).flatMap(_.as[PaginatedNewPostNotificationsDTO]).toTry
      notificationT.get.items.size should be(1)
      val notif = notificationT.get.items.head
      notif.topicId should be(topicId)
      notif.isRead should be(true)
    }

  }

  test("that fetching a topic gives us posts with nested replies") {
    val user1Token = "user1"
    val user1Id    = "user1Id"
    val user2Token = "user2"
    val user2Id    = "user2Id"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(user1Token)))).thenReturn(Success(user1Id))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(user2Token)))).thenReturn(Success(user2Id))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)
    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val createTopicRes = createTopic("title1", "description1", categoryId, token = user1Token)
    val topicIdT       = io.circe.parser.parse(createTopicRes.body).flatMap(_.as[api.TopicDTO]).toTry
    val topicId        = topicIdT.get.id

    val post1    = createPost("noe innhold i topicen", topicId, token = user2Token)
    val post1Try = io.circe.parser.parse(post1.body).flatMap(_.as[api.PostDTO]).toTry
    val post2    = createPost("noe annet innhold", topicId, token = user1Token, toPostId = Some(post1Try.get.id))
    val post2Try = io.circe.parser.parse(post2.body).flatMap(_.as[api.PostDTO]).toTry

    val post3 = createPost("Svar til noe annet innhold", topicId, token = user2Token, toPostId = Some(post2Try.get.id))
    val post3Try = io.circe.parser.parse(post3.body).flatMap(_.as[api.PostDTO]).toTry

    val topic1 = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/topics/$topicId")
        .header("FeideAuthorization", s"Bearer $user1Token")
        .readTimeout(10.seconds)
    )
    val topicTry = io.circe.parser.parse(topic1.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry.get

    topicTry.postCount should be(4)
    topicTry.posts.items.length should be(2)
    topicTry.posts.items.head.replies.length should be(0)
    topicTry.posts.items.last.replies.length should be(1)
    topicTry.posts.items.last.replies.asInstanceOf[List[api.PostDTO]].last.replies.last should be(post3Try.get)

  }

  test("that deleting post also deletes replies") {
    val user1Token = "user1"
    val user1Id    = "user1Id"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(user1Token)))).thenReturn(Success(user1Id))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)
    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val createTopicRes = createTopic("title1", "description1", categoryId, token = user1Token)
    val topicIdT       = io.circe.parser.parse(createTopicRes.body).flatMap(_.as[api.TopicDTO]).toTry
    val topicId        = topicIdT.get.id

    val post1    = createPost("noe innhold i topicen", topicId, token = user1Token)
    val post1Try = io.circe.parser.parse(post1.body).flatMap(_.as[api.PostDTO]).toTry
    createPost("noe annet innhold", topicId, token = user1Token, toPostId = Some(post1Try.get.id))

    val topic1 = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/topics/$topicId")
        .header("FeideAuthorization", s"Bearer $user1Token")
        .readTimeout(10.seconds)
    )

    val topicTry = io.circe.parser.parse(topic1.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry.get

    topicTry.postCount should be(3)

    val postDelete = simpleHttpClient.send(
      quickRequest
        .delete(uri"$myndlaApiArenaUrl/posts/${post1Try.get.id}")
        .header("FeideAuthorization", s"Bearer $user1Token")
        .readTimeout(10.seconds)
    )

    postDelete.code.code should be(200)

    val topic2 = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/topics/$topicId")
        .header("FeideAuthorization", s"Bearer $user1Token")
        .readTimeout(10.seconds)
    )

    val topic2Try = io.circe.parser.parse(topic2.body).flatMap(_.as[api.TopicWithPostsDTO]).toTry.get

    topic2Try.postCount should be(1)

  }

  test("that a post can get upvoted and the upvote can be removed again") {
    val userToken = "user"
    val userId    = "userId"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(userToken)))).thenReturn(Success(userId))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val topic   = createTopic("Topic title", "Topic description", categoryId, token = userToken)
    val topicT  = io.circe.parser.parse(topic.body).flatMap(_.as[api.TopicDTO]).toTry
    val topicId = topicT.get.id

    val ownPost   = createPost("Innhold i posten", topicId, token = userToken)
    val ownPostT  = io.circe.parser.parse(ownPost.body).flatMap(_.as[api.PostDTO]).toTry
    val ownPostId = ownPostT.get.id

    val otherPost   = createPost("Innhold i posten", topicId, toPostId = Some(ownPostId))
    val otherPostT  = CirceUtil.tryParseAs[api.PostDTO](otherPost.body).get
    val otherPostId = otherPostT.id

    val upvoteOwnPost = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/posts/$ownPostId/upvote")
        .header("FeideAuthorization", s"Bearer $userToken")
        .readTimeout(10.seconds)
    )

    val upvoteOwnPostT = CirceUtil.tryParseAs[api.PostDTO](upvoteOwnPost.body).get

    upvoteOwnPost.code.code should be(200)
    upvoteOwnPostT.upvotes should be(0)
    upvoteOwnPostT.upvoted should be(false)

    val firstUpvote = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/posts/$otherPostId/upvote")
        .header("FeideAuthorization", s"Bearer $userToken")
        .readTimeout(10.seconds)
    )

    val firstUpvoteT = CirceUtil.tryParseAs[api.PostDTO](firstUpvote.body).get

    firstUpvote.code.code should be(200)
    firstUpvoteT.upvotes should be(1)
    firstUpvoteT.upvoted should be(true)

    val secondUpvote = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/posts/$otherPostId/upvote")
        .header("FeideAuthorization", s"Bearer $userToken")
        .readTimeout(10.seconds)
    )

    secondUpvote.code.code should be(409)

    val unUpvote = simpleHttpClient.send(
      quickRequest
        .delete(uri"$myndlaApiArenaUrl/posts/$otherPostId/upvote")
        .header("FeideAuthorization", s"Bearer $userToken")
        .readTimeout(10.seconds)
    )

    val unUpvoteT = CirceUtil.tryParseAs[api.PostDTO](unUpvote.body).get

    unUpvote.code.code should be(200)
    unUpvoteT.upvotes should be(0)
    unUpvoteT.upvoted should be(false)
  }

  test("that a post can be flagged and flagged posts can be fetched and resolved") {
    val adminToken   = "adminToken"
    val adminId      = "adminId"
    val userOneToken = "userOneToken"
    val userOneId    = "userOneId"
    val userTwoToken = "userTwoToken"
    val userTwoId    = "userTwoId"

    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(adminToken)))).thenReturn(Success(adminId))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(userOneToken)))).thenReturn(Success(userOneId))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(userTwoToken)))).thenReturn(Success(userTwoId))

    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.CategoryDTO]).toTry
    val categoryId        = categoryIdT.get.id

    val topic   = createTopic("Topic title", "Topic description", categoryId, token = userOneToken)
    val topicT  = io.circe.parser.parse(topic.body).flatMap(_.as[api.TopicDTO]).toTry
    val topicId = topicT.get.id

    val ownPost   = createPost("Innhold i posten", topicId, token = userOneToken)
    val ownPostT  = io.circe.parser.parse(ownPost.body).flatMap(_.as[api.PostDTO]).toTry
    val ownPostId = ownPostT.get.id

    val flagPost = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/posts/$ownPostId/flag")
        .body("""{"reason": "spam"}""")
        .header("Content-Type", "application/json")
        .header("FeideAuthorization", s"Bearer $userTwoToken")
        .readTimeout(10.seconds)
    )
    flagPost.code.code should be(200)

    val flaggedPostsResponse = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/flags")
        .header("FeideAuthorization", s"Bearer $adminToken")
        .readTimeout(10.seconds)
    )

    val flaggedPosts = CirceUtil.unsafeParseAs[PaginatedPostsDTO](flaggedPostsResponse.body)
    flaggedPosts.totalCount should be(1)
    val p = flaggedPosts.items.head
    p.id should be(ownPostId)
    val flag = p.flags.get.head
    flag.reason should be("spam")

    simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/flags/${flag.id}")
        .header("FeideAuthorization", s"Bearer $adminToken")
        .readTimeout(10.seconds)
    )

    val flaggedPostsResponse2 = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/flags")
        .header("FeideAuthorization", s"Bearer $adminToken")
        .readTimeout(10.seconds)
    )

    val flaggedPosts2 = CirceUtil.unsafeParseAs[PaginatedPostsDTO](flaggedPostsResponse2.body)
    flaggedPosts2.totalCount should be(1)
    flaggedPosts2.items.head.flags.get.head.resolved.isDefined should be(true)
  }

  test("that a category cannot be moved into itself or a child") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaGroups(any)).thenReturn(List(ArenaGroup.ADMIN))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val category1   = createCategory("title1", "description1", parentCategoryId = None)
    val category1T  = io.circe.parser.parse(category1.body).flatMap(_.as[api.CategoryDTO]).toTry
    val category1Id = category1T.get.id

    val category2   = createCategory("title2", "description2", parentCategoryId = Some(category1Id))
    val category2T  = io.circe.parser.parse(category2.body).flatMap(_.as[api.CategoryDTO]).toTry
    val category2Id = category2T.get.id

    val category3   = createCategory("title3", "description3", parentCategoryId = Some(category2Id))
    val category3T  = io.circe.parser.parse(category3.body).flatMap(_.as[api.CategoryDTO]).toTry
    val category3Id = category3T.get.id

    val intoItself = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/categories/$category1Id")
        .body(
          CirceUtil.toJsonString(
            NewCategoryDTO(
              title = "title1",
              description = "description1",
              visible = true,
              parentCategoryId = Some(category1Id)
            )
          )
        )
    )
    intoItself.code.code should be(400)

    val intoChild = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/categories/$category1Id")
        .body(
          CirceUtil.toJsonString(
            NewCategoryDTO(
              title = "title1",
              description = "description1",
              visible = true,
              parentCategoryId = Some(category2Id)
            )
          )
        )
    )
    intoChild.code.code should be(400)

    val intoNestedChild = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/categories/$category1Id")
        .body(
          CirceUtil.toJsonString(
            NewCategoryDTO(
              title = "title1",
              description = "description1",
              visible = true,
              parentCategoryId = Some(category3Id)
            )
          )
        )
    )
    intoNestedChild.code.code should be(400)

    val intoAnotherParent = simpleHttpClient.send(
      quickRequest
        .put(uri"$myndlaApiArenaUrl/categories/$category3Id")
        .body(
          CirceUtil.toJsonString(
            NewCategoryDTO(
              title = "title3",
              description = "description3",
              visible = true,
              parentCategoryId = Some(category1Id)
            )
          )
        )
    )
    intoAnotherParent.code.code should be(200)

    val fetchCategoriesResponse = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/categories")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )

    val categories = io.circe.parser.parse(fetchCategoriesResponse.body).flatMap(_.as[List[api.CategoryDTO]]).toTry.get
    categories.size should be(1)
    categories.head.id should be(category1Id)
    categories.head.subcategories.map(_.id) should be(List(category2Id, category3Id))
  }

}
