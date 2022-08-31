/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import no.ndla.learningpathapi.db.migrationwithdependencies.V15__MergeDuplicateLanguageFields
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V15__MergeDuplicateLanguageFieldsTest extends UnitSuite with TestEnvironment {
  val migration = new V15__MergeDuplicateLanguageFields(props)

  test("Duplicate language fields are removed if same content") {
    val original =
      """{"tags":[{"tags":["produkt","prosess","sal"],"language":"nn"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"und"},{"tags":["product","process","sale"],"language":"en"},{"tags":["produkt","prosess","sal"],"language":"nn"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"und"},{"tags":["product","process","sale"],"language":"en"}],"owner":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","title":[{"title":"Produkterogproduktetslivssyklus","language":"nb"},{"title":"Produktoglivssyklusentilprodukt","language":"nn"}],"status":"PUBLISHED","duration":240,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"LiveMarieToftSundbye","type":"Redaksjonelt"}]},"description":[{"language":"nb","description":"Hvaviimarkedsføringmenermedprodukt,oghvabegrepeneproduktetslivssyklus,produktsortimentogproduktnivåerbetyr."},{"language":"nn","description":"Kvaviimarknadsføringmeinermedprodukt,ogkvaomgrepalivssyklus,produktsortimentogproduktnivåtyder."}],"lastUpdated":"2020-07-28T12:23:33Z","coverPhotoId":"21954","verificationStatus":"CREATED_BY_NDLA"}"""
    val expected =
      """{"tags":[{"tags":["product","process","sale"],"language":"und"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"en"},{"tags":["produkt","prosess","sal"],"language":"nn"}],"owner":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","title":[{"title":"Produkterogproduktetslivssyklus","language":"nb"},{"title":"Produktoglivssyklusentilprodukt","language":"nn"}],"status":"PUBLISHED","duration":240,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"LiveMarieToftSundbye","type":"Redaksjonelt"}]},"description":[{"language":"nb","description":"Hvaviimarkedsføringmenermedprodukt,oghvabegrepeneproduktetslivssyklus,produktsortimentogproduktnivåerbetyr."},{"language":"nn","description":"Kvaviimarknadsføringmeinermedprodukt,ogkvaomgrepalivssyklus,produktsortimentogproduktnivåtyder."}],"lastUpdated":"2020-07-28T12:23:33Z","coverPhotoId":"21954","verificationStatus":"CREATED_BY_NDLA"}"""

    migration.convertLearningPathDocument(1, original) should be(expected)
  }

  test("Duplicate language fields are removed if not same content, but tags are merged") {
    val original =
      """{"tags":[{"tags":["produkt","prosess","sal"],"language":"nn"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"und"},{"tags":["product","process","sale"],"language":"en"},{"tags":["produkt","papito","sal"],"language":"nn"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"und"},{"tags":["product","process","sale"],"language":"en"}],"owner":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","title":[{"title":"Produkterogproduktetslivssyklus","language":"nb"},{"title":"En rar tittel","language":"nb"},{"title":"Produktoglivssyklusentilprodukt","language":"nn"}],"status":"PUBLISHED","duration":240,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"LiveMarieToftSundbye","type":"Redaksjonelt"}]},"description":[{"language":"nb","description":"Hvaviimarkedsføringmenermedprodukt,oghvabegrepeneproduktetslivssyklus,produktsortimentogproduktnivåerbetyr."},{"language":"nn","description":"Kvaviimarknadsføringmeinermedprodukt,ogkvaomgrepalivssyklus,produktsortimentogproduktnivåtyder."}],"lastUpdated":"2020-07-28T12:23:33Z","coverPhotoId":"21954","verificationStatus":"CREATED_BY_NDLA"}"""
    val expected =
      """{"tags":[{"tags":["product","process","sale"],"language":"und"},{"tags":["produkt","prosess","salg"],"language":"nb"},{"tags":["product","process","sale"],"language":"en"},{"tags":["produkt","papito","sal","prosess"],"language":"nn"}],"owner":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","title":[{"title":"Produkterogproduktetslivssyklus","language":"nb"},{"title":"Produktoglivssyklusentilprodukt","language":"nn"}],"status":"PUBLISHED","duration":240,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"LiveMarieToftSundbye","type":"Redaksjonelt"}]},"description":[{"language":"nb","description":"Hvaviimarkedsføringmenermedprodukt,oghvabegrepeneproduktetslivssyklus,produktsortimentogproduktnivåerbetyr."},{"language":"nn","description":"Kvaviimarknadsføringmeinermedprodukt,ogkvaomgrepalivssyklus,produktsortimentogproduktnivåtyder."}],"lastUpdated":"2020-07-28T12:23:33Z","coverPhotoId":"21954","verificationStatus":"CREATED_BY_NDLA"}"""

    migration.convertLearningPathDocument(1, original) should be(expected)
  }
}
