/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.common.model.api as commonApi
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License.{CC_BY_NC_SA, PublicDomain}
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.NdlaHttpRequest
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester

import scala.util.Success

class ImageSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment
    with PrivateMethodTester {
  import TestData.searchSettings
  import props.{DefaultPageSize, MaxPageSize}

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val searchConverterService = new SearchConverterService
  override val converterService       = new ConverterService
  override val imageIndexService: ImageIndexService = new ImageIndexService {
    override val indexShards = 1
  }
  override val imageSearchService = new ImageSearchService

  val getStartAtAndNumResults: PrivateMethod[(Int, Int)] = PrivateMethod[(Int, Int)](Symbol("getStartAtAndNumResults"))

  val largeImage: ImageFileData = ImageFileData(1, "large-full-url", 10000, "jpg", None, "und", 4)
  val smallImage: ImageFileData = ImageFileData(2, "small-full-url", 100, "jpg", None, "und", 6)
  val podcastImage: ImageFileData =
    ImageFileData(3, "podcast-full-url", 100, "jpg", Some(ImageDimensions(width = 1400, height = 1400)), "und", 6)

  val byNcSa: Copyright = Copyright(
    CC_BY_NC_SA.toString,
    Some("Gotham City"),
    List(Author("Forfatter", "DC Comics")),
    List(),
    List(),
    None,
    None,
    false
  )

  val publicDomain: Copyright = Copyright(
    PublicDomain.toString,
    Some("Metropolis"),
    List(Author("Forfatter", "Bruce Wayne")),
    List(),
    List(),
    None,
    None,
    false
  )
  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val agreement1Copyright: commonApi.CopyrightDTO = commonApi.CopyrightDTO(
    commonApi.LicenseDTO("gnu", Some("gnustuff"), Some("http://gnugnusen")),
    Some("Simsalabim"),
    List(),
    List(),
    List(),
    None,
    None,
    false
  )

  val image1 = new ImageMetaInformation(
    id = Some(1),
    titles = List(ImageTitle("Batmen er på vift med en bil", "nb")),
    alttexts = List(ImageAltText("Bilde av en bil flaggermusmann som vifter med vingene bil.", "nb")),
    images = Some(Seq(largeImage)),
    copyright = byNcSa,
    tags = List(Tag(List("fugl"), "nb")),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.NO,
    editorNotes = Seq.empty
  )

  val image2 = new ImageMetaInformation(
    id = Some(2),
    titles = List(ImageTitle("Pingvinen er ute og går", "nb")),
    alttexts = List(ImageAltText("Bilde av en en pingvin som vagger borover en gate.", "nb")),
    images = Some(Seq(largeImage)),
    copyright = publicDomain,
    tags = List(Tag(List("fugl"), "nb")),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.NOT_APPLICABLE,
    editorNotes = Seq(EditorNote(NDLADate.now(), "someone", "Lillehjelper"))
  )

  val image3 = new ImageMetaInformation(
    id = Some(3),
    titles = List(ImageTitle("Donald Duck kjører bil", "nb")),
    alttexts = List(ImageAltText("Bilde av en en and som kjører en rød bil.", "nb")),
    images = Some(Seq(smallImage)),
    copyright = byNcSa,
    tags = List(Tag(List("and"), "nb")),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  val image4 = new ImageMetaInformation(
    id = Some(4),
    titles = List(ImageTitle("Hulken er ute og lukter på blomstene", "und")),
    alttexts = Seq(),
    images = Some(Seq(smallImage)),
    copyright = byNcSa,
    tags = Seq(),
    captions = Seq(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  val image5 = new ImageMetaInformation(
    id = Some(5),
    titles = List(
      ImageTitle("Dette er et urelatert bilde", "und"),
      ImageTitle("This is a unrelated photo", "en"),
      ImageTitle("Nynoreg", "nn")
    ),
    alttexts = Seq(ImageAltText("urelatert alttext", "und"), ImageAltText("Nynoreg", "nn")),
    images = Some(Seq(podcastImage)),
    copyright = byNcSa,
    tags = Seq(),
    captions = Seq(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      val indexName = imageIndexService.createIndexWithGeneratedName
      imageIndexService.updateAliasTarget(None, indexName.get)

      imageIndexService.indexDocument(image1).get
      imageIndexService.indexDocument(image2).get
      imageIndexService.indexDocument(image3).get
      imageIndexService.indexDocument(image4).get
      imageIndexService.indexDocument(image5).get

      val servletRequest = mock[NdlaHttpRequest]
      when(servletRequest.getHeader(any[String])).thenReturn(Some("http"))
      when(servletRequest.serverName).thenReturn("localhost")
      when(servletRequest.servletPath).thenReturn("/image-api/v2/images/")
      ApplicationUrl.set(servletRequest)

      blockUntil(() => imageSearchService.countDocuments() == 5)
    }
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    imageSearchService invokePrivate getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    imageSearchService invokePrivate getStartAtAndNumResults(None, Some(10001)) should equal((0, MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size"
  ) {
    val page            = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    imageSearchService invokePrivate getStartAtAndNumResults(Some(page), None) should equal(
      (expectedStartAt, DefaultPageSize)
    )
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 123
    val pageSize        = 43
    val expectedStartAt = (page - 1) * pageSize
    imageSearchService invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal(
      (expectedStartAt, pageSize)
    )
  }

  test("That all returns all documents ordered by id ascending") {
    val searchResult = imageSearchService.matchingQuery(searchSettings.copy(), None).get
    searchResult.totalCount should be(5)
    searchResult.results.size should be(5)
    searchResult.page.get should be(1)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("5")
  }

  test("That all filtering on minimumsize only returns images larger than minimumsize") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(minimumSize = Some(500)), None)
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("2")
  }

  test("That all filtering on license only returns images with given license") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(license = Some(PublicDomain.toString)), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(searchResultPage1) =
      imageSearchService.matchingQuery(searchSettings.copy(page = Some(1), pageSize = Some(2)), None)
    val Success(searchResultPage2) =
      imageSearchService.matchingQuery(searchSettings.copy(page = Some(2), pageSize = Some(2)), None)
    searchResultPage1.totalCount should be(5)
    searchResultPage1.page.get should be(1)
    searchResultPage1.pageSize should be(2)
    searchResultPage1.results.size should be(2)
    searchResultPage1.results.head.id should be("1")
    searchResultPage1.results.last.id should be("2")

    searchResultPage2.totalCount should be(5)
    searchResultPage2.page.get should be(2)
    searchResultPage2.pageSize should be(2)
    searchResultPage2.results.size should be(2)
    searchResultPage2.results.head.id should be("3")
    searchResultPage2.results.last.id should be("4")
  }

  test("That both minimum-size and license filters are applied.") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(
        searchSettings.copy(minimumSize = Some(500), license = Some(PublicDomain.toString)),
        None
      )
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches title and alttext ordered by relevance") {
    val res                   = imageSearchService.matchingQuery(searchSettings.copy(query = Some("bil")), None)
    val Success(searchResult) = res
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("3")
  }

  test("That search matches title") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("Pingvinen"), language = "nb"), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches id search") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("1"), language = "nb"), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("1")
  }

  test("That search on author matches corresponding author on image") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(query = Some("Bruce Wayne")), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches tags") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("and"), language = "nb"), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("3")
  }

  test("That search defaults to nb if no language is specified") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("Bilde av en and")), None)
    searchResult.totalCount should be(4)
    searchResult.results.size should be(4)
    searchResult.results.head.id should be("1")
    searchResult.results(1).id should be("2")
    searchResult.results(2).id should be("3")
    searchResult.results.last.id should be("5")
  }

  test("That search matches title with unknown language analyzed in Norwegian") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(query = Some("blomstene")), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("4")
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      imageSearchService.matchingQuery(
        searchSettings.copy(
          query = Some("batmen AND bil"),
          language = "nb",
          page = Some(1),
          pageSize = Some(10)
        ),
        None
      )
    search1.results.map(_.id) should equal(Seq("1", "3"))

    val Success(search2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen | pingvinen"),
        language = "nb",
        page = Some(1),
        pageSize = Some(10)
      ),
      None
    )
    search2.results.map(_.id) should equal(Seq("1", "2"))

    val Success(search3) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("bilde + -flaggermusmann"),
        language = "nb",
        page = Some(1),
        pageSize = Some(10)
      ),
      None
    )
    search3.results.map(_.id) should equal(Seq("2", "3"))

    val Success(search4) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen + bil"),
        language = "nb",
        page = Some(1),
        pageSize = Some(10)
      ),
      None
    )
    search4.results.map(_.id) should equal(Seq("1"))
  }

  test("Searching for multiple languages should returned matched language") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("urelatert"),
        language = "*"
      ),
      None
    )
    searchResult1.totalCount should be(1)
    searchResult1.results.size should be(1)
    searchResult1.results.head.id should be("5")
    searchResult1.results.head.title.language should equal("und")
    searchResult1.results.head.altText.language should equal("und")

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("unrelated"),
        language = "*",
        sort = Sort.ByTitleDesc
      ),
      None
    )
    searchResult2.totalCount should be(1)
    searchResult2.results.size should be(1)
    searchResult2.results.head.id should be("5")
    searchResult2.results.head.title.language should equal("en")
    searchResult2.results.head.altText.language should equal("nn")
  }

  test("Searching for unused languages should returned nothing") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "ait" // Arikem
      ),
      None
    )
    searchResult1.totalCount should be(0)
  }

  test("That field should be returned in another language if match does not contain searchLanguage") {
    val Success(searchResult) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("unrelated"),
        language = "en"
      ),
      None
    )
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("5")
    searchResult.results.head.title.language should equal("en")
    searchResult.results.head.altText.language should equal("nn")

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("nynoreg"),
        language = "nn"
      ),
      None
    )
    searchResult2.totalCount should be(1)
    searchResult2.results.size should be(1)
    searchResult2.results.head.id should be("5")
    searchResult2.results.head.title.language should equal("nn")
    searchResult2.results.head.altText.language should equal("nn")
  }

  test("That supportedLanguages returns in order") {
    val Success(result) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("nynoreg"),
        language = "nn"
      ),
      None
    )
    result.totalCount should be(1)
    result.results.size should be(1)

    result.results.head.supportedLanguages should be(Seq("nn", "en", "und"))
  }

  test("That scrolling works as expected") {
    val pageSize    = 2
    val expectedIds = List("1", "2", "3", "4", "5").sliding(pageSize, pageSize).toList

    val Success(initialSearch) = imageSearchService.matchingQuery(
      searchSettings.copy(
        pageSize = Some(pageSize),
        shouldScroll = true
      ),
      None
    )

    val Success(scroll1) = imageSearchService.scrollV2(initialSearch.scrollId.get, "*", None)
    val Success(scroll2) = imageSearchService.scrollV2(scroll1.scrollId.get, "*", None)
    val Success(scroll3) = imageSearchService.scrollV2(scroll2.scrollId.get, "*", None)

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(List.empty)
  }

  test("That scrolling v3 works as expected") {
    val pageSize    = 2
    val expectedIds = List[Long](1, 2, 3, 4, 5).sliding(pageSize, pageSize).toList

    val Success(initialSearch) = imageSearchService.matchingQueryV3(
      searchSettings.copy(
        pageSize = Some(pageSize),
        shouldScroll = true
      ),
      None
    )

    val Success(scroll1) = imageSearchService.scroll(initialSearch.scrollId.get, "*")
    val Success(scroll2) = imageSearchService.scroll(scroll1.scrollId.get, "*")
    val Success(scroll3) = imageSearchService.scroll(scroll2.scrollId.get, "*")

    initialSearch.results.map(_._1.id) should be(expectedIds.head)
    scroll1.results.map(_._1.id) should be(expectedIds(1))
    scroll2.results.map(_._1.id) should be(expectedIds(2))
    scroll3.results.map(_._1.id) should be(List.empty)
  }

  test("That title search works as expected, and doesn't crash in combination with language") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "nb",
        sort = Sort.ByTitleDesc
      ),
      None
    )

    searchResult1.results.map(_.id) should be(Seq("2", "3", "1"))
  }

  test("That searching for notes only works for editors") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("lillehjelper"),
        language = "*"
      ),
      None
    )

    searchResult1.results.map(_.id) should be(Seq())

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("lillehjelper"),
        language = "*"
      ),
      Some(TokenUser("someeditor", Set(IMAGE_API_WRITE), None))
    )

    searchResult2.results.map(_.id) should be(Seq("2"))
  }

  test("That filtering for modelReleased works as expected") {
    import ModelReleasedStatus._
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        modelReleased = Seq(NO)
      ),
      None
    )

    searchResult1.results.map(_.id) should be(Seq("1"))

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        modelReleased = Seq(NOT_APPLICABLE)
      ),
      None
    )

    searchResult2.results.map(_.id) should be(Seq("2"))

    val Success(searchResult3) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        modelReleased = Seq(YES)
      ),
      None
    )

    searchResult3.results.map(_.id) should be(Seq("3", "4", "5"))

    val Success(searchResult4) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        modelReleased = Seq.empty
      ),
      None
    )

    searchResult4.results.map(_.id) should be(Seq("1", "2", "3", "4", "5"))

    val Success(searchResult5) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        modelReleased = Seq(NO, NOT_APPLICABLE)
      ),
      None
    )

    searchResult5.results.map(_.id) should be(Seq("1", "2"))

  }

  test("That search result includes updatedBy field") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("1"), language = "nb"), None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.lastUpdated should be(updated)

  }

  test("Searching for languages with fallback should return result in specified language") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("urelatert"),
        language = "und"
      ),
      None
    )
    searchResult1.totalCount should be(1)
    searchResult1.results.size should be(1)
    searchResult1.results.head.id should be("5")
    searchResult1.results.head.title.title should equal("Dette er et urelatert bilde")
    searchResult1.results.head.title.language should equal("und")

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("unrelated"),
        language = "en"
      ),
      None
    )
    searchResult2.totalCount should be(1)
    searchResult2.results.size should be(1)
    searchResult2.results.head.id should be("5")
    searchResult2.results.head.title.title should equal("This is a unrelated photo")
    searchResult2.results.head.title.language should equal("en")
  }

  test("That filtering for podcast-friendly works as expected") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        podcastFriendly = Some(true)
      ),
      None
    )

    searchResult1.results.map(_.id) should be(Seq("5"))
  }
}
