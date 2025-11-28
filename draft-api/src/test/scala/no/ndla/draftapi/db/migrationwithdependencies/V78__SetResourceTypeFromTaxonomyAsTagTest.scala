/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import io.circe.parser
import no.ndla.common.model.api.search.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.common.model.taxonomy.*
import no.ndla.draftapi.db.migrationwithdependencies.V78__SetResourceTypeFromTaxonomyAsTag
import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.Success

class V78__SetResourceTypeFromTaxonomyAsTagTest extends UnitSuite with TestEnvironment {

  val parent = ContextResourceType(
    id = "urn:resourcetype:parent",
    parentId = None,
    name = SearchableLanguageValues(Seq(LanguageValue("nb", "Forelder"))),
  )
  val child = ContextResourceType(
    id = "urn:resourcetype:child",
    parentId = Some("urn:resourcetype:parent"),
    name = SearchableLanguageValues(Seq(LanguageValue("nb", "Barn"))),
  )

  val context_1: TaxonomyContext = TaxonomyContext(
    publicId = "urn:resource:1",
    rootId = "urn:subject:1",
    root = SearchableLanguageValues(Seq(LanguageValue("nb", "Artikkel"))),
    path = "/subject:1/resource:1",
    breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
    contextType = Some("standard"),
    relevanceId = "urn:relevance:1",
    relevance = SearchableLanguageValues(Seq.empty),
    resourceTypes = List(parent, child),
    parentIds = List.empty,
    isPrimary = true,
    contextId = "asdf1234",
    isVisible = true,
    isActive = true,
    url = "/f/matte/asdf1234",
  )

  val node = Node(
    id = "urn:resource:1",
    name = "Video",
    contentUri = Some("urn:article:1"),
    path = None,
    url = None,
    metadata = None,
    translations = List(),
    nodeType = NodeType.RESOURCE,
    resourceTypes = List(),
    contextids = List(),
    context = Some(context_1),
    contexts = List(context_1),
  )

  test("That article gets updated with tags from taxonomy") {

    when(baseTaxonomyApiClient.getTaxonomyBundleUncached(any[Boolean])).thenReturn(
      Success(TaxonomyBundle(nodes = List(node)))
    )

    val migration = new V78__SetResourceTypeFromTaxonomyAsTag()(using baseTaxonomyApiClient)

    val oldDocument = """
        |{
        |  "id": 1,
        |  "tags": [
        |    {
        |      "tags": ["nøkkelord"],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "Title",
        |      "language": "nb"
        |    }
        |  ],
        |  "content": [
        |    {
        |      "content": "<section>Content</section>",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2017-05-29T09:43:41.000Z",
        |  "updated": "2017-07-18T10:21:08.000Z",
        |  "revision": 1,
        |  "copyright": {
        |    "origin": "",
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "Forfatter",
        |        "name": "Sissel Paaske"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "Supplier",
        |        "name": "Cerpus AS"
        |      }
        |    ]
        |  },
        |  "grepCodes": [],
        |  "metaImage": [],
        |  "published": "2017-07-18T10:21:08.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "standard",
        |  "availability": "everyone",
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "Introduction."
        |    }
        |  ],
        |  "revisionDate": "2030-01-01T00:00:00.000Z",
        |  "visualElement": [],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Metabeskrivelse",
        |      "language": "nb"
        |    }
        |  ],
        |  "requiredLibraries": []
        |}
        |""".stripMargin

    val newDocument = """
        |{
        |  "id": 1,
        |  "tags": [
        |    {
        |      "tags": ["nøkkelord", "Barn"],
        |      "language": "nb"
        |    }
        |  ],
        |  "title": [
        |    {
        |      "title": "Title",
        |      "language": "nb"
        |    }
        |  ],
        |  "content": [
        |    {
        |      "content": "<section>Content</section>",
        |      "language": "nb"
        |    }
        |  ],
        |  "created": "2017-05-29T09:43:41.000Z",
        |  "updated": "2017-07-18T10:21:08.000Z",
        |  "revision": 1,
        |  "copyright": {
        |    "origin": "",
        |    "license": "CC-BY-SA-4.0",
        |    "validTo": null,
        |    "processed": false,
        |    "validFrom": null,
        |    "creators": [
        |      {
        |        "type": "Forfatter",
        |        "name": "Sissel Paaske"
        |      }
        |    ],
        |    "processors": [],
        |    "rightsholders": [
        |      {
        |        "type": "Supplier",
        |        "name": "Cerpus AS"
        |      }
        |    ]
        |  },
        |  "grepCodes": [],
        |  "metaImage": [],
        |  "published": "2017-07-18T10:21:08.000Z",
        |  "updatedBy": "r0gHb9Xg3li4yyXv0QSGQczV3bviakrT",
        |  "conceptIds": [],
        |  "disclaimer": {},
        |  "articleType": "standard",
        |  "availability": "everyone",
        |  "introduction": [
        |    {
        |      "language": "nb",
        |      "introduction": "Introduction."
        |    }
        |  ],
        |  "revisionDate": "2030-01-01T00:00:00.000Z",
        |  "visualElement": [],
        |  "relatedContent": [],
        |  "metaDescription": [
        |    {
        |      "content": "Metabeskrivelse",
        |      "language": "nb"
        |    }
        |  ],
        |  "requiredLibraries": []
        |}
        |""".stripMargin

    val expected = parser.parse(newDocument).toTry.get
    migration.convertColumn(1, oldDocument) should be(expected.noSpaces)
  }
}
