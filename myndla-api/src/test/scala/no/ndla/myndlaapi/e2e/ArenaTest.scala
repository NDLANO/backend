/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.arena.api
import no.ndla.myndlaapi._
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.quality.Strictness
import org.testcontainers.containers.PostgreSQLContainer
import sttp.client3.Response
import sttp.client3.quick._

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
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val redisPort: Int              = redisContainer.get.port
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"

    override def LpMetaServer: String      = pgc.getHost
    override def LpMetaResource: String    = pgc.getDatabaseName
    override def LpMetaUserName: String    = pgc.getUsername
    override def LpMetaPassword: String    = pgc.getPassword
    override def LpMetaPort: Int           = pgc.getMappedPort(5432)
    override def LpMetaSchema: String      = "testschema"
    override def migrateToLocalDB: Boolean = true

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = redisPort
  }

  val someDate = NDLADate.of(2017, 1, 1, 1, 59)
  val feideId  = "feide"

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override lazy val clock = mock[SystemClock](withSettings.strictness(Strictness.LENIENT))
      override lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override lazy val userRepository: UserRepository     = spy(new UserRepository)
      override lazy val userService: UserService           = spy(new UserService)
      override lazy val configService: ConfigService       = spy(new ConfigService)

      when(feideApiClient.getFeideID(any)).thenReturn(Success("q"))
      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any))
        .thenReturn(Success(FeideExtendedUserInfo("", Seq("employee"), "email@ndla.no", Seq("email@ndla.no"))))
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(configService.getMyNDLAEnabledOrgs).thenReturn(Success(List("zxc")))
      when(clock.now()).thenReturn(someDate)
    }
  }

  val testClock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl  = s"http://localhost:$myndlaApiPort"
  val myndlaApiArenaUrl = s"$myndlaApiBaseUrl/myndla-api/v1/arena"

  override def beforeAll(): Unit = {
    IO { myndlaApi.run() }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(myndlaApi.componentRegistry.userService)
    reset(myndlaApi.componentRegistry.userRepository)

    myndlaApi.componentRegistry.arenaRepository.withSession(implicit session => {
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

  val testUser = MyNDLAUser(
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
    arenaAdmin = Some(false),
    shareName = false
  )

  val testAdmin = MyNDLAUser(
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
    arenaAdmin = Some(true),
    shareName = false
  )

  def createCategory(title: String, description: String, shouldSucceed: Boolean = true): Response[String] = {
    val newCategory = api.NewCategory(title = title, description = description)
    val inBody      = newCategory.asJson.noSpaces
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
      shouldSucceed: Boolean = true
  ): Response[String] = {
    val newTopic = api.NewTopic(title = title, initialPost = api.NewPost(content = content))
    val inBody   = newTopic.asJson.noSpaces
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/categories/$categoryId/topics")
        .body(inBody)
        .header("Content-type", "application/json")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    res
  }

  def createPost(content: String, topicId: Long, shouldSucceed: Boolean = true): Response[String] = {
    val newPost = api.NewPost(content = content)
    val inBody  = newPost.asJson.noSpaces
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiArenaUrl/topics/$topicId/posts")
        .body(inBody)
        .header("Content-type", "application/json")
        .header("FeideAuthorization", s"Bearer asd")
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

    val categories = io.circe.parser.parse(fetchCategoriesResponse.body).flatMap(_.as[List[api.Category]]).toTry.get
    categories.size should be(1)
    categories.head should be(api.Category(1, "title", "description", 0, 0))
  }

  test("that creating a category with a bunch of topics and posts works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(myndlaApi.componentRegistry.userService.getInitialIsArenaAdmin(any)).thenReturn(Some(true))
    when(myndlaApi.componentRegistry.clock.now()).thenReturn(someDate)

    val createCategoryRes = createCategory("title", "description")
    val categoryIdT       = io.circe.parser.parse(createCategoryRes.body).flatMap(_.as[api.Category]).toTry
    val categoryId        = categoryIdT.get.id

    val top1 = createTopic("title1", "description1", categoryId)
    createTopic("title2", "description2", categoryId)
    createTopic("title3", "description3", categoryId)

    val top1T  = io.circe.parser.parse(top1.body).flatMap(_.as[api.Topic]).toTry
    val top1Id = top1T.get.id

    createPost("post1", top1Id)
    createPost("post2", top1Id)
    createPost("post3", top1Id)
    createPost("post4", top1Id)

    val owner = api.Owner(1, "")

    val expectedResult = api.CategoryWithTopics(
      id = 1,
      title = "title",
      description = "description",
      topicCount = 3,
      postCount = 7,
      topicPage = 1,
      topicPageSize = 10,
      topics = List(
        api.Topic(
          id = 1,
          title = "title1",
          postCount = 5,
//          posts = List(
//            api.Post(
//              id = 1,
//              content = "description1",
//              created = someDate,
//              updated = someDate,
//              owner = owner,
//              flags = Some(List.empty)
//            ),
//            api
//              .Post(
//                id = 4,
//                content = "post1",
//                created = someDate,
//                updated = someDate,
//                owner = owner,
//                flags = Some(List.empty)
//              ),
//            api
//              .Post(
//                id = 5,
//                content = "post2",
//                created = someDate,
//                updated = someDate,
//                owner = owner,
//                flags = Some(List.empty)
//              ),
//            api
//              .Post(
//                id = 6,
//                content = "post3",
//                created = someDate,
//                updated = someDate,
//                owner = owner,
//                flags = Some(List.empty)
//              ),
//            api.Post(
//              id = 7,
//              content = "post4",
//              created = someDate,
//              updated = someDate,
//              owner = owner,
//              flags = Some(List.empty)
//            )
//          ),
          created = someDate,
          updated = someDate
        ),
        api.Topic(
          id = 2,
          title = "title2",
          postCount = 1,
//          posts = List(
//            api.Post(
//              id = 2,
//              content = "description2",
//              created = someDate,
//              updated = someDate,
//              owner = owner,
//              flags = Some(List.empty)
//            )
//          ),
          created = someDate,
          updated = someDate
        ),
        api.Topic(
          id = 3,
          title = "title3",
          postCount = 1,
//          posts = List(
//            api.Post(
//              id = 3,
//              content = "description3",
//              created = someDate,
//              updated = someDate,
//              owner = owner,
//              flags = Some(List.empty)
//            )
//          ),
          created = someDate,
          updated = someDate
        )
      )
    )

    val categoryResp = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiArenaUrl/categories/1")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )

    categoryResp.code.code should be(200)

    val resultTry = io.circe.parser.parse(categoryResp.body).flatMap(_.as[api.CategoryWithTopics]).toTry
    resultTry should be(Success(expectedResult))
  }

}
