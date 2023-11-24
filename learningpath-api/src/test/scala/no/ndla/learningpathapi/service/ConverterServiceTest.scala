/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.errors.ValidationException
import no.ndla.common.model.{NDLADate, api => commonApi}
import no.ndla.common.model.domain.learningpath.{EmbedType, EmbedUrl, LearningpathCopyright}
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.learningpathapi.integration.ImageMetaInformation
import no.ndla.learningpathapi.model.api.{
  CoverPhoto,
  MyNDLAGroup,
  NewCopyLearningPathV2,
  NewLearningPathV2,
  NewLearningStepV2
}
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{TestData, UnitSuite, UnitTestEnvironment}
import no.ndla.mapping.License.CC_BY
import no.ndla.network.ApplicationUrl
import no.ndla.network.clients.{FeideGroup, Membership}
import no.ndla.network.tapir.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH, LEARNINGPATH_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers._
import org.mockito.Strictness

import java.util.UUID
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with UnitTestEnvironment {
  import props.DefaultLanguage
  val clinton = commonApi.Author("author", "Crooked Hillary")
  val license = commonApi.License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright = api.Copyright(license, List(clinton))

  val apiLearningPath = api.LearningPathV2(
    1,
    1,
    None,
    api.Title("Tittel", "nb"),
    api.Description("Beskrivelse", "nb"),
    "",
    List(),
    "",
    None,
    None,
    "PRIVATE",
    "CREATED_BY_NDLA",
    NDLADate.now(),
    api.LearningPathTags(List(), "nb"),
    copyright,
    true,
    List("nb"),
    None,
    None
  )
  val domainLearningStep = LearningStep(None, None, None, None, 1, List(), List(), List(), StepType.INTRODUCTION, None)

  val domainLearningStep2 = LearningStep(
    Some(1),
    Some(1),
    None,
    None,
    1,
    List(Title("tittel", "nb")),
    List(Description("deskripsjon", "nb")),
    List(),
    StepType.INTRODUCTION,
    None
  )
  val apiTags = List(api.LearningPathTags(Seq("tag"), DefaultLanguage))

  val randomDate                = NDLADate.now()
  var service: ConverterService = _

  val domainLearningPath = LearningPath(
    Some(1),
    Some(1),
    None,
    None,
    List(Title("tittel", DefaultLanguage)),
    List(Description("deskripsjon", DefaultLanguage)),
    None,
    Some(60),
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.CREATED_BY_NDLA,
    randomDate,
    List(Tag(List("tag"), DefaultLanguage)),
    "me",
    LearningpathCopyright(CC_BY.toString, List.empty),
    None
  )

  override def beforeEach() = {
    service = new ConverterService
  }

  test("asApiLearningpathV2 converts domain to api LearningPathV2") {
    val expected = Success(
      api.LearningPathV2(
        1,
        1,
        None,
        api.Title("tittel", DefaultLanguage),
        api.Description("deskripsjon", DefaultLanguage),
        "null1",
        List.empty,
        "null1/learningsteps",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
        randomDate,
        api.LearningPathTags(Seq("tag"), DefaultLanguage),
        api.Copyright(
          commonApi.License(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          ),
          List.empty
        ),
        canEdit = true,
        List("nb", "en"),
        None,
        None
      )
    )
    service.asApiLearningpathV2(
      domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
      DefaultLanguage,
      false,
      TokenUser("me", Set.empty, None)
    ) should equal(expected)
  }

  test("asApiLearningpathV2 returns Failure if fallback is false and language is not supported") {
    service.asApiLearningpathV2(
      domainLearningPath,
      "hurr-durr-lang",
      false,
      TokenUser("me", Set.empty, None)
    ) should equal(
      Failure(NotFoundException("Language 'hurr-durr-lang' is not supported for learningpath with id '1'."))
    )
  }

  test("asApiLearningpathV2 converts domain to api LearningPathV2 with fallback if true") {
    val expected = Success(
      api.LearningPathV2(
        1,
        1,
        None,
        api.Title("tittel", DefaultLanguage),
        api.Description("deskripsjon", DefaultLanguage),
        "null1",
        List.empty,
        "null1/learningsteps",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
        randomDate,
        api.LearningPathTags(Seq("tag"), DefaultLanguage),
        api.Copyright(
          commonApi.License(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          ),
          List.empty
        ),
        true,
        List("nb", "en"),
        None,
        None
      )
    )
    service.asApiLearningpathV2(
      domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
      "hurr durr I'm a language",
      true,
      TokenUser("me", Set.empty, None)
    ) should equal(expected)
  }

  test("asApiLearningpathSummaryV2 converts domain to api LearningpathSummaryV2") {
    val expected = Success(
      api.LearningPathSummaryV2(
        1,
        Some(1),
        api.Title("tittel", DefaultLanguage),
        api.Description("deskripsjon", DefaultLanguage),
        api.Introduction("", DefaultLanguage),
        "null1",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        randomDate,
        api.LearningPathTags(Seq("tag"), DefaultLanguage),
        api.Copyright(
          commonApi.License(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          ),
          List.empty
        ),
        List("nb", "en"),
        None,
        None
      )
    )
    service.asApiLearningpathSummaryV2(
      domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
      TokenUser.PublicUser
    ) should equal(expected)
  }

  test("asApiLearningStepV2 converts domain learningstep to api LearningStepV2") {
    val learningstep = Success(
      api.LearningStepV2(
        1,
        1,
        1,
        api.Title("tittel", DefaultLanguage),
        Some(api.Description("deskripsjon", DefaultLanguage)),
        None,
        showTitle = false,
        "INTRODUCTION",
        None,
        "null1/learningsteps/1",
        canEdit = true,
        "ACTIVE",
        Seq(DefaultLanguage)
      )
    )
    service.asApiLearningStepV2(
      domainLearningStep2,
      domainLearningPath,
      DefaultLanguage,
      false,
      TokenUser("me", Set.empty, None)
    ) should equal(learningstep)
  }

  test("asApiLearningStepV2 return Failure if fallback is false and language not supported") {
    service.asApiLearningStepV2(
      domainLearningStep2,
      domainLearningPath,
      "hurr durr I'm a language",
      false,
      TokenUser("me", Set.empty, None)
    ) should equal(
      Failure(
        NotFoundException(
          "Learningstep with id '1' in learningPath '1' and language hurr durr I'm a language not found."
        )
      )
    )
  }

  test(
    "asApiLearningStepV2 converts domain learningstep to api LearningStepV2 if fallback is true and language undefined"
  ) {
    val learningstep = Success(
      api.LearningStepV2(
        1,
        1,
        1,
        api.Title("tittel", DefaultLanguage),
        Some(api.Description("deskripsjon", DefaultLanguage)),
        None,
        showTitle = false,
        "INTRODUCTION",
        None,
        "null1/learningsteps/1",
        canEdit = true,
        "ACTIVE",
        Seq(DefaultLanguage)
      )
    )
    service.asApiLearningStepV2(
      domainLearningStep2,
      domainLearningPath,
      "hurr durr I'm a language",
      true,
      TokenUser("me", Set.empty, None)
    ) should equal(learningstep)
  }

  test("asApiLearningStepSummaryV2 converts domain learningstep to LearningStepSummaryV2") {
    val expected = Some(
      api.LearningStepSummaryV2(
        1,
        1,
        api.Title("tittel", DefaultLanguage),
        "INTRODUCTION",
        "null1/learningsteps/1"
      )
    )

    service.asApiLearningStepSummaryV2(domainLearningStep2, domainLearningPath, DefaultLanguage) should equal(expected)
  }

  test("asApiLearningStepSummaryV2 returns what we have when not supported language is given") {
    val expected = Some(
      api.LearningStepSummaryV2(
        1,
        1,
        api.Title("tittel", DefaultLanguage),
        "INTRODUCTION",
        "null1/learningsteps/1"
      )
    )

    service.asApiLearningStepSummaryV2(domainLearningStep2, domainLearningPath, "somerandomlanguage") should equal(
      expected
    )
  }

  test("asApiLearningPathTagsSummary converts api LearningPathTags to api LearningPathTagsSummary") {
    val expected =
      Some(api.LearningPathTagsSummary(DefaultLanguage, Seq(DefaultLanguage), Seq("tag")))
    service.asApiLearningPathTagsSummary(apiTags, DefaultLanguage, false) should equal(expected)
  }

  test("asApiLearningPathTagsSummary returns None if fallback is false and language is unsupported") {
    service.asApiLearningPathTagsSummary(apiTags, "hurr durr I'm a language", false) should equal(None)
  }

  test(
    "asApiLearningPathTagsSummary converts api LearningPathTags to api LearningPathTagsSummary if language is undefined and fallback is true"
  ) {
    val expected =
      Some(api.LearningPathTagsSummary(DefaultLanguage, Seq(DefaultLanguage), Seq("tag")))
    service.asApiLearningPathTagsSummary(apiTags, "hurr durr I'm a language", true) should equal(expected)
  }

  test("That createUrlToLearningPath does not include private in path for private learningpath") {
    val httpServletRequest = mock[HttpServletRequest](withSettings.strictness(Strictness.Lenient))
    when(httpServletRequest.getServerPort).thenReturn(80)
    when(httpServletRequest.getScheme).thenReturn("http")
    when(httpServletRequest.getServerName).thenReturn("api-gateway.ndla-local")
    when(httpServletRequest.getServletPath).thenReturn("/servlet")
    when(httpServletRequest.getHeader(anyString)).thenReturn(null)

    ApplicationUrl.set(httpServletRequest)
    service.createUrlToLearningPath(apiLearningPath.copy(status = "PRIVATE")) should equal(
      s"${props.Domain}/servlet/1"
    )
  }

  test("That asApiIntroduction returns an introduction for a given step") {
    val introductions = service.getApiIntroduction(
      Seq(
        domainLearningStep.copy(
          description = Seq(
            Description("Introduksjon på bokmål", "nb"),
            Description("Introduksjon på nynorsk", "nn"),
            Description("Introduction in english", "en")
          )
        )
      )
    )

    introductions.size should be(3)
    introductions.find(_.language.contains("nb")).map(_.introduction) should be(Some("Introduksjon på bokmål"))
    introductions.find(_.language.contains("nn")).map(_.introduction) should be(Some("Introduksjon på nynorsk"))
    introductions.find(_.language.contains("en")).map(_.introduction) should be(Some("Introduction in english"))
  }

  test("That asApiIntroduction returns empty list if no descriptions are available") {
    val introductions = service.getApiIntroduction(Seq(domainLearningStep))
    introductions.size should be(0)
  }

  test("That asApiIntroduction returns an empty list if given a None") {
    service.getApiIntroduction(Seq()) should equal(Seq())
  }

  test("asApiLicense returns a License object for a given valid license") {
    service.asApiLicense("CC-BY-4.0") should equal(
      commonApi.License(
        CC_BY.toString,
        Option("Creative Commons Attribution 4.0 International"),
        Some("https://creativecommons.org/licenses/by/4.0/")
      )
    )
  }

  test("asApiLicense returns a default license object for an invalid license") {
    service.asApiLicense("invalid") should equal(commonApi.License("invalid", Option("Invalid license"), None))
  }

  test("asEmbedUrl returns embedUrl if embedType is oembed") {
    service.asEmbedUrlV2(api.EmbedUrlV2("http://test.no/2/oembed/", "oembed"), "nb") should equal(
      EmbedUrl("http://test.no/2/oembed/", "nb", EmbedType.OEmbed)
    )
  }

  test("asEmbedUrl throws error if an not allowed value for embedType is used") {
    assertResult("Validation Error") {
      intercept[ValidationException] {
        service.asEmbedUrlV2(api.EmbedUrlV2("http://test.no/2/oembed/", "test"), "nb")
      }.getMessage()
    }
  }

  test("asCoverPhoto converts an image id to CoverPhoto") {
    val imageMeta =
      ImageMetaInformation(
        "1",
        "http://image-api.ndla-local/image-api/v2/images/1",
        "http://image-api.ndla-local/image-api/raw/1.jpg",
        1024,
        "something"
      )
    val expectedResult =
      CoverPhoto(
        "http://api-gateway.ndla-local/image-api/raw/1.jpg",
        "http://api-gateway.ndla-local/image-api/v2/images/1"
      )
    when(imageApiClient.imageMetaOnUrl(any[String])).thenReturn(Some(imageMeta))
    val Some(result) = service.asCoverPhoto("1")

    result should equal(expectedResult)
  }

  test("asDomainEmbed should only use context path if hostname is ndla-frontend but full url when not") {
    val url = "https://ndla.no/subjects/resource:1234?a=test"
    when(oembedProxyClient.getIframeUrl(eqTo(url))).thenReturn(Success(url))
    service.asDomainEmbedUrl(api.EmbedUrlV2(url, "oembed"), "nb") should equal(
      Success(EmbedUrl(s"/subjects/resource:1234?a=test", "nb", EmbedType.IFrame))
    )

    val externalUrl = "https://youtube.com/watch?v=8992BFHks"
    service.asDomainEmbedUrl(api.EmbedUrlV2(externalUrl, "oembed"), "nb") should equal(
      Success(EmbedUrl(externalUrl, "nb", EmbedType.OEmbed))
    )
  }

  test("That a apiLearningPath should only contain ownerId if admin") {
    val noAdmin =
      service.asApiLearningpathV2(domainLearningPath, "nb", false, TokenUser(domainLearningPath.owner, Set.empty, None))
    val admin =
      service.asApiLearningpathV2(
        domainLearningPath,
        "nb",
        false,
        TokenUser("kwakk", Set(LEARNINGPATH_API_ADMIN), None)
      )

    noAdmin.get.ownerId should be(None)
    admin.get.ownerId.get should be(domainLearningPath.owner)
  }

  test("New learningPaths get correct verification") {
    val apiRubio = commonApi.Author("author", "Little Marco")
    val apiLicense =
      commonApi.License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
    val apiCopyright = api.Copyright(apiLicense, List(apiRubio))

    val newCopyLp = NewCopyLearningPathV2("Tittel", Some("Beskrivelse"), "nb", None, Some(1), None, None)
    val newLp     = NewLearningPathV2("Tittel", "Beskrivelse", None, Some(1), List(), "nb", apiCopyright)

    service
      .newFromExistingLearningPath(domainLearningPath, newCopyLp, TokenUser("Me", Set.empty, None))
      .verificationStatus should be(LearningPathVerificationStatus.EXTERNAL)
    service.newLearningPath(newLp, TokenUser("Me", Set.empty, None)).verificationStatus should be(
      LearningPathVerificationStatus.EXTERNAL
    )
    service
      .newFromExistingLearningPath(domainLearningPath, newCopyLp, TokenUser("Me", Set(LEARNINGPATH_API_ADMIN), None))
      .verificationStatus should be(LearningPathVerificationStatus.CREATED_BY_NDLA)
    service.newLearningPath(newLp, TokenUser("Me", Set(LEARNINGPATH_API_PUBLISH), None)).verificationStatus should be(
      LearningPathVerificationStatus.CREATED_BY_NDLA
    )
    service.newLearningPath(newLp, TokenUser("Me", Set(LEARNINGPATH_API_WRITE), None)).verificationStatus should be(
      LearningPathVerificationStatus.CREATED_BY_NDLA
    )
  }

  test("asDomainLearningStep should work with learningpaths no matter the amount of steps") {
    val newLs =
      NewLearningStepV2("Tittel", Some("Beskrivelse"), "nb", Some(api.EmbedUrlV2("", "oembed")), true, "TEXT", None)
    val lpId = 5591L
    val lp1  = TestData.sampleDomainLearningPath.copy(id = Some(lpId), learningsteps = None)
    val lp2  = TestData.sampleDomainLearningPath.copy(id = Some(lpId), learningsteps = Some(Seq.empty))
    val lp3 = TestData.sampleDomainLearningPath.copy(
      id = Some(lpId),
      learningsteps =
        Some(Seq(TestData.domainLearningStep1.copy(seqNo = 0), TestData.domainLearningStep2.copy(seqNo = 1)))
    )

    service.asDomainLearningStep(newLs, lp1).get.seqNo should be(0)
    service.asDomainLearningStep(newLs, lp2).get.seqNo should be(0)
    service.asDomainLearningStep(newLs, lp3).get.seqNo should be(2)
  }

  test("toNewFolderData transforms correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val newFolder1 = api.NewFolder(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("private"),
      description = None
    )
    val newFolder2 = api.NewFolder(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("shared"),
      description = Some("descc")
    )
    val newFolder3 =
      api.NewFolder(
        name = "kenkaku",
        parentId = Some(folderUUID.toString),
        status = Some("ikkeesksisterendestatus"),
        description = Some("")
      )

    val expected1 = domain.NewFolderData(
      parentId = Some(folderUUID),
      name = "kenkaku",
      status = domain.FolderStatus.PRIVATE,
      rank = None,
      description = None
    )

    service.toNewFolderData(newFolder1, Some(folderUUID), None).get should be(expected1)
    service.toNewFolderData(newFolder2, Some(folderUUID), None).get should be(
      expected1.copy(status = domain.FolderStatus.SHARED, description = Some("descc"))
    )
    service.toNewFolderData(newFolder3, Some(folderUUID), None).get should be(
      expected1.copy(status = domain.FolderStatus.PRIVATE, description = Some(""))
    )
  }

  test("toApiFolder transforms correctly when data isn't corrupted") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val mainFolderUUID = UUID.randomUUID()
    val subFolder1UUID = UUID.randomUUID()
    val subFolder2UUID = UUID.randomUUID()
    val subFolder3UUID = UUID.randomUUID()
    val resourceUUID   = UUID.randomUUID()

    val resource =
      domain.Resource(
        id = resourceUUID,
        feideId = "w",
        resourceType = "concept",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        connection = None
      )
    val folderData1 = domain.Folder(
      id = subFolder1UUID,
      feideId = "u",
      parentId = Some(subFolder3UUID),
      name = "folderData1",
      status = domain.FolderStatus.PRIVATE,
      resources = List(resource),
      subfolders = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1")
    )
    val folderData2 = domain.Folder(
      id = subFolder2UUID,
      feideId = "w",
      parentId = Some(mainFolderUUID),
      name = "folderData2",
      status = domain.FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2")
    )
    val folderData3 = domain.Folder(
      id = subFolder3UUID,
      feideId = "u",
      parentId = Some(mainFolderUUID),
      name = "folderData3",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List(folderData1),
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3")
    )
    val mainFolder = domain.Folder(
      id = mainFolderUUID,
      feideId = "u",
      parentId = None,
      name = "mainFolder",
      status = domain.FolderStatus.SHARED,
      subfolders = List(folderData2, folderData3),
      resources = List(resource),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder")
    )
    val apiResource = api.Resource(
      id = resourceUUID.toString,
      resourceType = "concept",
      tags = List("a", "b", "c"),
      created = created,
      path = "/subject/1/topic/1/resource/4",
      resourceId = "1",
      rank = None
    )
    val apiData1 = api.Folder(
      id = subFolder1UUID.toString,
      name = "folderData1",
      status = "private",
      resources = List(apiResource),
      subfolders = List(),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder3UUID.toString, name = "folderData3"),
        api.Breadcrumb(id = subFolder1UUID.toString, name = "folderData1")
      ),
      parentId = Some(subFolder3UUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1"),
      owner = None
    )
    val apiData2 = api.Folder(
      id = subFolder2UUID.toString,
      name = "folderData2",
      status = "shared",
      resources = List.empty,
      subfolders = List.empty,
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder2UUID.toString, name = "folderData2")
      ),
      parentId = Some(mainFolderUUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2"),
      owner = None
    )
    val apiData3 = api.Folder(
      id = subFolder3UUID.toString,
      name = "folderData3",
      status = "private",
      subfolders = List(apiData1),
      resources = List(),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder3UUID.toString, name = "folderData3")
      ),
      parentId = Some(mainFolderUUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3"),
      owner = None
    )
    val expected = api.Folder(
      id = mainFolderUUID.toString,
      name = "mainFolder",
      status = "shared",
      subfolders = List(apiData2, apiData3),
      resources = List(apiResource),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")
      ),
      parentId = None,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder"),
      owner = None
    )

    val Success(result) =
      service.toApiFolder(mainFolder, List(api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")), None)
    result should be(expected)
  }

  test("updateFolder updates folder correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val parentUUID = UUID.randomUUID()

    val existing = domain.Folder(
      id = folderUUID,
      feideId = "u",
      parentId = Some(parentUUID),
      name = "folderData1",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      shared = None,
      description = Some("hei")
    )
    val updatedWithData =
      api.UpdatedFolder(name = Some("newNamae"), status = Some("shared"), description = Some("halla"))
    val updatedWithoutData = api.UpdatedFolder(name = None, status = None, description = None)
    val updatedWithGarbageData =
      api.UpdatedFolder(
        name = Some("huehueuheasdasd+++"),
        status = Some("det å joike er noe kult"),
        description = Some("jog ska visa deg garbage jog")
      )

    val expected1 =
      existing.copy(name = "newNamae", status = FolderStatus.SHARED, shared = Some(shared), description = Some("halla"))
    val expected2 = existing.copy(name = "folderData1", status = FolderStatus.PRIVATE)
    val expected3 = existing.copy(
      name = "huehueuheasdasd+++",
      status = FolderStatus.PRIVATE,
      description = Some("jog ska visa deg garbage jog")
    )

    val result1 = service.mergeFolder(existing, updatedWithData)
    val result2 = service.mergeFolder(existing, updatedWithoutData)
    val result3 = service.mergeFolder(existing, updatedWithGarbageData)

    result1 should be(expected1)
    result2 should be(expected2)
    result3 should be(expected3)
  }

  test("that mergeFolder works correctly for shared field and folder status update") {
    val sharedBefore = NDLADate.now().minusDays(1)
    val sharedNow    = NDLADate.now()
    when(clock.now()).thenReturn(sharedNow)

    val existingBase = domain.Folder(
      id = UUID.randomUUID(),
      feideId = "u",
      parentId = Some(UUID.randomUUID()),
      name = "folderData1",
      status = domain.FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      shared = Some(sharedBefore),
      description = None
    )
    val existingShared  = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val existingPrivate = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val updatedShared   = api.UpdatedFolder(name = None, status = Some("shared"), description = None)
    val updatedPrivate  = api.UpdatedFolder(name = None, status = Some("private"), description = None)
    val expected1       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val expected2       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val expected3       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedNow))
    val expected4       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)

    val result1 = service.mergeFolder(existingShared, updatedShared)
    val result2 = service.mergeFolder(existingShared, updatedPrivate)
    val result3 = service.mergeFolder(existingPrivate, updatedShared)
    val result4 = service.mergeFolder(existingPrivate, updatedPrivate)
    result1 should be(expected1)
    result2 should be(expected2)
    result3 should be(expected3)
    result4 should be(expected4)
  }

  test("that toApiResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val folderUUID = UUID.randomUUID()

    val existing =
      domain.Resource(
        id = folderUUID,
        feideId = "feideid",
        resourceType = "article",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        connection = None
      )
    val expected =
      api.Resource(
        id = folderUUID.toString,
        resourceType = "article",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        rank = None
      )

    service.toApiResource(existing) should be(Success(expected))
  }

  test("that newResource toDomainResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val newResource1 =
      api.NewResource(
        resourceType = "audio",
        path = "/subject/1/topic/1/resource/4",
        tags = Some(List("a", "b")),
        resourceId = "1"
      )
    val newResource2 =
      api.NewResource(resourceType = "audio", path = "/subject/1/topic/1/resource/4", tags = None, resourceId = "2")
    val expected1 =
      domain.ResourceDocument(
        tags = List("a", "b"),
        resourceId = "1"
      )
    val expected2 = expected1.copy(tags = List.empty, resourceId = "2")

    service.toDomainResource(newResource1) should be(expected1)
    service.toDomainResource(newResource2) should be(expected2)
  }

  test("That domainToApimodel transforms Folder from domain to api model correctly") {
    val folder1UUID = UUID.randomUUID()
    val folder2UUID = UUID.randomUUID()
    val folder3UUID = UUID.randomUUID()

    val folderDomainList = List(
      TestData.emptyDomainFolder.copy(id = folder1UUID),
      TestData.emptyDomainFolder.copy(id = folder2UUID),
      TestData.emptyDomainFolder.copy(id = folder3UUID)
    )

    val result = service.domainToApiModel(folderDomainList, f => converterService.toApiFolder(f, List.empty, None))
    result.get.length should be(3)
    result should be(
      Success(
        List(
          TestData.emptyApiFolder.copy(id = folder1UUID.toString, status = "private"),
          TestData.emptyApiFolder.copy(id = folder2UUID.toString, status = "private"),
          TestData.emptyApiFolder.copy(id = folder3UUID.toString, status = "private")
        )
      )
    )
  }

  test("That toApiUserData works correctly") {
    val domainUserData =
      domain.MyNDLAUser(
        id = 42,
        feideId = "feide",
        favoriteSubjects = Seq("a", "b"),
        userRole = UserRole.STUDENT,
        lastUpdated = clock.now(),
        organization = "oslo",
        groups = Seq(
          FeideGroup(id = "id", displayName = "oslo", membership = Membership(primarySchool = None), parent = None)
        ),
        email = "example@email.com",
        arenaEnabled = false,
        displayName = "Feide",
        shareName = false
      )
    val expectedUserData =
      api.MyNDLAUser(
        id = 42,
        username = "example@email.com",
        displayName = "Feide",
        favoriteSubjects = Seq("a", "b"),
        role = "student",
        organization = "oslo",
        groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
        arenaEnabled = false,
        shareName = false
      )

    service.toApiUserData(domainUserData, List.empty) should be(expectedUserData)
  }

  test("That mergeUserData works correctly") {
    val domainUserData = domain.MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups =
        Seq(FeideGroup(id = "id", displayName = "oslo", membership = Membership(primarySchool = None), parent = None)),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val updatedUserData1 = api.UpdatedMyNDLAUser(favoriteSubjects = None, arenaEnabled = None, shareName = None)
    val updatedUserData2 =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq.empty), arenaEnabled = None, shareName = None)
    val updatedUserData3 =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq("x", "y", "z")), arenaEnabled = None, shareName = None)

    val expectedUserData1 = domain.MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups =
        Seq(FeideGroup(id = "id", displayName = "oslo", membership = Membership(primarySchool = None), parent = None)),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val expectedUserData2 = domain.MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq.empty,
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups =
        Seq(FeideGroup(id = "id", displayName = "oslo", membership = Membership(primarySchool = None), parent = None)),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val expectedUserData3 = domain.MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("x", "y", "z"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups =
        Seq(FeideGroup(id = "id", displayName = "oslo", membership = Membership(primarySchool = None), parent = None)),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )

    service.mergeUserData(domainUserData, updatedUserData1, None) should be(expectedUserData1)
    service.mergeUserData(domainUserData, updatedUserData2, None) should be(expectedUserData2)
    service.mergeUserData(domainUserData, updatedUserData3, None) should be(expectedUserData3)
  }
}
