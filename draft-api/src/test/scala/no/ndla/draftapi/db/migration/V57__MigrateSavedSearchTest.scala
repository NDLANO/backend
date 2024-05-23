package no.ndla.draftapi.db.migration

import io.circe.syntax.EncoderOps
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V57__MigrateSavedSearchTest extends UnitSuite with TestEnvironment {
  test("Saved search migration test") {
    val migration = new V57__MigrateSavedSearch
    val result = migration.convertDocument(
      """{"userId": "testesen",  "savedSearches": ["/search/content?fallback=false&page-size=10&language=en&query=hallo&resource-types=topic-article&subjects=urn%3Asubject%3A1%3A83ce68bc-19c9-4f2b-8dba-caf401428f21&sort=-lastUpdated","/search/audio?page-size=10&resource-types=urn%3Aresourcetype%3AfilmClip&query=hei%20p%C3%A5%20deg&sort=-lastUpdated"], "favoriteSubjects": ["urn:subject:1:f7c5f36a-198d-4c38-a330-2957cf1a8325", "urn:subject:1:11c4696f-e844-4c98-8df7-49d43f59ec33"], "latestEditedArticles": ["37595", "37498"]}""".stripMargin
    )
    val expected = V57_UserData(
      userId = "testesen",
      savedSearches = Some(
        List(
          V57_SavedSearch(
            "/search/content?fallback=false&page-size=10&language=en&query=hallo&resource-types=topic-article&subjects=urn%3Asubject%3A1%3A83ce68bc-19c9-4f2b-8dba-caf401428f21&sort=-lastUpdated",
            "Innhold + Biologi 1 + \"hallo\" + Engelsk + Emne"
          ),
          V57_SavedSearch(
            "/search/audio?page-size=10&resource-types=urn%3Aresourcetype%3AfilmClip&query=hei%20p%C3%A5%20deg&sort=-lastUpdated",
            "Lyd + Filmklipp + \"hei på deg\""
          )
        )
      ),
      favoriteSubjects = Some(
        List(
          "urn:subject:1:f7c5f36a-198d-4c38-a330-2957cf1a8325",
          "urn:subject:1:11c4696f-e844-4c98-8df7-49d43f59ec33"
        )
      ),
      latestEditedArticles = Some(List("37595", "37498")),
      latestEditedConcepts = None
    )
    assert(result == expected.asJson.noSpaces)
  }
}
