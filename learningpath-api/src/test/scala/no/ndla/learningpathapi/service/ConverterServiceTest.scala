/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.errors.{NotFoundException, ValidationException}
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepType
}
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api.{
  CoverPhotoDTO,
  NewCopyLearningPathV2DTO,
  NewLearningPathV2DTO,
  NewLearningStepV2DTO
}
import no.ndla.learningpathapi.{TestData, UnitSuite, UnitTestEnvironment}
import no.ndla.mapping.License.CC_BY
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.NdlaHttpRequest
import no.ndla.network.tapir.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH, LEARNINGPATH_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness

import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with UnitTestEnvironment {
  import props.DefaultLanguage
  val clinton: commonApi.AuthorDTO = commonApi.AuthorDTO("author", "Crooked Hillary")
  val license: commonApi.LicenseDTO =
    commonApi.LicenseDTO("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright: api.CopyrightDTO = api.CopyrightDTO(license, List(clinton))

  val apiLearningPath: api.LearningPathV2DTO = api.LearningPathV2DTO(
    1,
    1,
    None,
    api.TitleDTO("Tittel", "nb"),
    api.DescriptionDTO("Beskrivelse", "nb"),
    "",
    List(),
    "",
    None,
    None,
    "PRIVATE",
    "CREATED_BY_NDLA",
    NDLADate.now(),
    NDLADate.now(),
    api.LearningPathTagsDTO(List(), "nb"),
    copyright,
    true,
    List("nb"),
    None,
    None,
    None
  )
  val domainLearningStep: LearningStep =
    LearningStep(None, None, None, None, 1, List(), List(), List(), List(), StepType.INTRODUCTION, None)

  val domainLearningStep2: LearningStep = LearningStep(
    Some(1),
    Some(1),
    None,
    None,
    1,
    List(Title("tittel", "nb")),
    List(),
    List(Description("deskripsjon", "nb")),
    List(),
    StepType.INTRODUCTION,
    None
  )
  val apiTags: List[api.LearningPathTagsDTO] = List(api.LearningPathTagsDTO(Seq("tag"), DefaultLanguage))

  val randomDate: NDLADate      = NDLADate.now()
  var service: ConverterService = _

  val domainLearningPath: LearningPath = LearningPath(
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
    randomDate,
    List(Tag(List("tag"), DefaultLanguage)),
    "me",
    LearningpathCopyright(CC_BY.toString, List.empty),
    None
  )

  override def beforeEach(): Unit = {
    service = new ConverterService
  }

  test("asApiLearningpathV2 converts domain to api LearningPathV2") {
    val expected = Success(
      api.LearningPathV2DTO(
        1,
        1,
        None,
        api.TitleDTO("tittel", DefaultLanguage),
        api.DescriptionDTO("deskripsjon", DefaultLanguage),
        "null1",
        List.empty,
        "null1/learningsteps",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
        randomDate,
        randomDate,
        api.LearningPathTagsDTO(Seq("tag"), DefaultLanguage),
        api.CopyrightDTO(
          commonApi.LicenseDTO(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          ),
          List.empty
        ),
        canEdit = true,
        List("nb", "en"),
        None,
        None,
        None
      )
    )
    service.asApiLearningpathV2(
      domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
      DefaultLanguage,
      false,
      TokenUser("me", Set.empty, None).toCombined
    ) should equal(expected)
  }

  test("asApiLearningpathV2 returns Failure if fallback is false and language is not supported") {
    service.asApiLearningpathV2(
      domainLearningPath,
      "hurr-durr-lang",
      false,
      TokenUser("me", Set.empty, None).toCombined
    ) should equal(
      Failure(NotFoundException("Language 'hurr-durr-lang' is not supported for learningpath with id '1'."))
    )
  }

  test("asApiLearningpathV2 converts domain to api LearningPathV2 with fallback if true") {
    val expected = Success(
      api.LearningPathV2DTO(
        1,
        1,
        None,
        api.TitleDTO("tittel", DefaultLanguage),
        api.DescriptionDTO("deskripsjon", DefaultLanguage),
        "null1",
        List.empty,
        "null1/learningsteps",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
        randomDate,
        randomDate,
        api.LearningPathTagsDTO(Seq("tag"), DefaultLanguage),
        api.CopyrightDTO(
          commonApi.LicenseDTO(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          ),
          List.empty
        ),
        true,
        List("nb", "en"),
        None,
        None,
        None
      )
    )
    service.asApiLearningpathV2(
      domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
      "hurr durr I'm a language",
      true,
      TokenUser("me", Set.empty, None).toCombined
    ) should equal(expected)
  }

  test("asApiLearningpathSummaryV2 converts domain to api LearningpathSummaryV2") {
    val expected = Success(
      api.LearningPathSummaryV2DTO(
        1,
        Some(1),
        api.TitleDTO("tittel", DefaultLanguage),
        api.DescriptionDTO("deskripsjon", DefaultLanguage),
        api.IntroductionDTO("", DefaultLanguage),
        "null1",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        randomDate,
        randomDate,
        api.LearningPathTagsDTO(Seq("tag"), DefaultLanguage),
        api.CopyrightDTO(
          commonApi.LicenseDTO(
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
      TokenUser.PublicUser.toCombined
    ) should equal(expected)
  }

  test("asApiLearningStepV2 converts domain learningstep to api LearningStepV2") {
    val learningstep = Success(
      api.LearningStepV2DTO(
        1,
        1,
        1,
        api.TitleDTO("tittel", DefaultLanguage),
        None,
        Some(api.DescriptionDTO("deskripsjon", DefaultLanguage)),
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
      TokenUser("me", Set.empty, None).toCombined
    ) should equal(learningstep)
  }

  test("asApiLearningStepV2 return Failure if fallback is false and language not supported") {
    service.asApiLearningStepV2(
      domainLearningStep2,
      domainLearningPath,
      "hurr durr I'm a language",
      false,
      TokenUser("me", Set.empty, None).toCombined
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
      api.LearningStepV2DTO(
        1,
        1,
        1,
        api.TitleDTO("tittel", DefaultLanguage),
        None,
        Some(api.DescriptionDTO("deskripsjon", DefaultLanguage)),
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
      TokenUser("me", Set.empty, None).toCombined
    ) should equal(learningstep)
  }

  test("asApiLearningStepSummaryV2 converts domain learningstep to LearningStepSummaryV2") {
    val expected = Some(
      api.LearningStepSummaryV2DTO(
        1,
        1,
        api.TitleDTO("tittel", DefaultLanguage),
        "INTRODUCTION",
        "null1/learningsteps/1"
      )
    )

    service.asApiLearningStepSummaryV2(domainLearningStep2, domainLearningPath, DefaultLanguage) should equal(expected)
  }

  test("asApiLearningStepSummaryV2 returns what we have when not supported language is given") {
    val expected = Some(
      api.LearningStepSummaryV2DTO(
        1,
        1,
        api.TitleDTO("tittel", DefaultLanguage),
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
      Some(api.LearningPathTagsSummaryDTO(DefaultLanguage, Seq(DefaultLanguage), Seq("tag")))
    service.asApiLearningPathTagsSummary(apiTags, DefaultLanguage, false) should equal(expected)
  }

  test("asApiLearningPathTagsSummary returns None if fallback is false and language is unsupported") {
    service.asApiLearningPathTagsSummary(apiTags, "hurr durr I'm a language", false) should equal(None)
  }

  test(
    "asApiLearningPathTagsSummary converts api LearningPathTags to api LearningPathTagsSummary if language is undefined and fallback is true"
  ) {
    val expected =
      Some(api.LearningPathTagsSummaryDTO(DefaultLanguage, Seq(DefaultLanguage), Seq("tag")))
    service.asApiLearningPathTagsSummary(apiTags, "hurr durr I'm a language", true) should equal(expected)
  }

  test("That createUrlToLearningPath does not include private in path for private learningpath") {
    val httpServletRequest = mock[NdlaHttpRequest](withSettings.strictness(Strictness.LENIENT))
    when(httpServletRequest.serverPort).thenReturn(80)
    when(httpServletRequest.getScheme).thenReturn("http")
    when(httpServletRequest.serverName).thenReturn("api-gateway.ndla-local")
    when(httpServletRequest.servletPath).thenReturn("/servlet")
    when(httpServletRequest.getHeader(any)).thenReturn(null)

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
      commonApi.LicenseDTO(
        CC_BY.toString,
        Option("Creative Commons Attribution 4.0 International"),
        Some("https://creativecommons.org/licenses/by/4.0/")
      )
    )
  }

  test("asApiLicense returns a default license object for an invalid license") {
    service.asApiLicense("invalid") should equal(commonApi.LicenseDTO("invalid", Option("Invalid license"), None))
  }

  test("asEmbedUrl returns embedUrl if embedType is oembed") {
    service.asEmbedUrlV2(api.EmbedUrlV2DTO("http://test.no/2/oembed/", "oembed"), "nb") should equal(
      EmbedUrl("http://test.no/2/oembed/", "nb", EmbedType.OEmbed)
    )
  }

  test("asEmbedUrl throws error if an not allowed value for embedType is used") {
    assertResult("Validation Error:\n\tembedType: 'test' is not a valid embed type.") {
      intercept[ValidationException] {
        service.asEmbedUrlV2(api.EmbedUrlV2DTO("http://test.no/2/oembed/", "test"), "nb")
      }.getMessage
    }
  }

  test("asCoverPhoto converts an image id to CoverPhoto") {
    val expectedResult =
      CoverPhotoDTO(
        s"${props.Domain}/image-api/raw/id/1",
        s"${props.Domain}/image-api/v3/images/1"
      )
    val Some(result) = service.asCoverPhoto("1")
    result should equal(expectedResult)
  }

  test("asDomainEmbed should only use context path if hostname is ndla-frontend but full url when not") {
    val url = "https://ndla.no/subjects/resource:1234?a=test"
    when(oembedProxyClient.getIframeUrl(eqTo(url))).thenReturn(Success(url))
    service.asDomainEmbedUrl(api.EmbedUrlV2DTO(url, "oembed"), "nb") should equal(
      Success(EmbedUrl(s"/subjects/resource:1234?a=test", "nb", EmbedType.IFrame))
    )

    val externalUrl = "https://youtube.com/watch?v=8992BFHks"
    service.asDomainEmbedUrl(api.EmbedUrlV2DTO(externalUrl, "oembed"), "nb") should equal(
      Success(EmbedUrl(externalUrl, "nb", EmbedType.OEmbed))
    )
  }

  test("That a apiLearningPath should only contain ownerId if admin") {
    val noAdmin =
      service.asApiLearningpathV2(
        domainLearningPath,
        "nb",
        false,
        TokenUser(domainLearningPath.owner, Set.empty, None).toCombined
      )
    val admin =
      service.asApiLearningpathV2(
        domainLearningPath,
        "nb",
        false,
        TokenUser("kwakk", Set(LEARNINGPATH_API_ADMIN), None).toCombined
      )

    noAdmin.get.ownerId should be(None)
    admin.get.ownerId.get should be(domainLearningPath.owner)
  }

  test("New learningPaths get correct verification") {
    val apiRubio = commonApi.AuthorDTO("author", "Little Marco")
    val apiLicense =
      commonApi.LicenseDTO("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
    val apiCopyright = api.CopyrightDTO(apiLicense, List(apiRubio))

    val newCopyLp = NewCopyLearningPathV2DTO("Tittel", Some("Beskrivelse"), "nb", None, Some(1), None, None)
    val newLp     = NewLearningPathV2DTO("Tittel", "Beskrivelse", None, Some(1), List(), "nb", apiCopyright)

    service
      .newFromExistingLearningPath(domainLearningPath, newCopyLp, TokenUser("Me", Set.empty, None).toCombined)
      .get
      .verificationStatus should be(LearningPathVerificationStatus.EXTERNAL)
    service.newLearningPath(newLp, TokenUser("Me", Set.empty, None).toCombined).get.verificationStatus should be(
      LearningPathVerificationStatus.EXTERNAL
    )
    service
      .newFromExistingLearningPath(
        domainLearningPath,
        newCopyLp,
        TokenUser("Me", Set(LEARNINGPATH_API_ADMIN), None).toCombined
      )
      .get
      .verificationStatus should be(LearningPathVerificationStatus.CREATED_BY_NDLA)
    service
      .newLearningPath(newLp, TokenUser("Me", Set(LEARNINGPATH_API_PUBLISH), None).toCombined)
      .get
      .verificationStatus should be(
      LearningPathVerificationStatus.CREATED_BY_NDLA
    )
    service
      .newLearningPath(newLp, TokenUser("Me", Set(LEARNINGPATH_API_WRITE), None).toCombined)
      .get
      .verificationStatus should be(
      LearningPathVerificationStatus.CREATED_BY_NDLA
    )
  }

  test("asDomainLearningStep should work with learningpaths no matter the amount of steps") {
    val newLs =
      NewLearningStepV2DTO(
        "Tittel",
        Some("Beskrivelse"),
        None,
        "nb",
        Some(api.EmbedUrlV2DTO("", "oembed")),
        true,
        "TEXT",
        None
      )
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

}
