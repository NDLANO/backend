/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V20__UpdateH5PDomainForFFTest extends UnitSuite with TestEnvironment {
  val migration = new V20__UpdateH5PDomainForFF

  test("article should get new h5p domains, and only that") {

    val old1 =
      """{"tags":[{"tags":["fine","tags"],"language":"nn"},{"tags":["super","mann"],"language":"nb"}],"notes":[],"title":[{"title":"Noreg er et land","language":"nn"},{"title":"Norge er et land","language":"nb"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><h1>Med h5p<embed data-resource=\"external\" data-url=\"https://h5p.ndla.no/resource/72e0c67c-a219-4263-acb6-pinnebrød\"> <section><embed data-resource=\"external\" data-url=\"https://h5p.ndla.no/resource/72e0c67c-apekatt-96d85bf0dc25\"></section></h1>Dette er en artikkel uten noe h5p</section>","language":"nn"}],"created":"2017-03-24T13:30:10Z","updated":"2017-08-21T21:30:16Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[{"name":"En Mann","type":"Writer"},{"name":"Ei dame","type":"Writer"}],"processors":[],"rightsholders":[]},"metaImage":[{"altText":"Er bor det mange dyr","imageId":"42831","language":"nn"},{"altText":"Du er en apekatt","imageId":"42831","language":"nb"}],"published":"2017-08-21T21:30:16Z","updatedBy":"EnKulId","articleType":"standard","introduction":[{"language":"nn","introduction":"\nEn ny intro jadda"},{"language":"nb","introduction":"\nNy intro igjen jadda"}],"visualElement":[],"metaDescription":[{"content":"En lang meta beskrivelse","language":"nn"},{"content":"En kort metabeskrivelse","language":"nb"}],"requiredLibraries":[]}"""
    val old2 =
      """{"tags":[{"tags":["fine","tags"],"language":"nn"},{"tags":["super","mann"],"language":"nb"}],"notes":[],"title":[{"title":"Noreg er et land","language":"nn"},{"title":"Norge er et land","language":"nb"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><h1>Uten h5p</h1>Dette er en artikkel uten noe h5p</section>","language":"nn"}],"created":"2017-03-24T13:30:10Z","updated":"2017-08-21T21:30:16Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[{"name":"En Mann","type":"Writer"},{"name":"Ei dame","type":"Writer"}],"processors":[],"rightsholders":[]},"metaImage":[{"altText":"Er bor det mange dyr","imageId":"42831","language":"nn"},{"altText":"Du er en apekatt","imageId":"42831","language":"nb"}],"published":"2017-08-21T21:30:16Z","updatedBy":"EnKulId","articleType":"standard","introduction":[{"language":"nn","introduction":"\nEn ny intro jadda"},{"language":"nb","introduction":"\nNy intro igjen jadda"}],"visualElement":[],"metaDescription":[{"content":"En lang meta beskrivelse","language":"nn"},{"content":"En kort metabeskrivelse","language":"nb"}],"requiredLibraries":[]}"""

    val expected1 =
      """{"tags":[{"tags":["fine","tags"],"language":"nn"},{"tags":["super","mann"],"language":"nb"}],"notes":[],"title":[{"title":"Noreg er et land","language":"nn"},{"title":"Norge er et land","language":"nb"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><h1>Med h5p<embed data-resource=\"external\" data-url=\"https://h5p-ff.ndla.no/resource/72e0c67c-a219-4263-acb6-pinnebrød\"> <section><embed data-resource=\"external\" data-url=\"https://h5p-ff.ndla.no/resource/72e0c67c-apekatt-96d85bf0dc25\"></section></h1>Dette er en artikkel uten noe h5p</section>","language":"nn"}],"created":"2017-03-24T13:30:10Z","updated":"2017-08-21T21:30:16Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[{"name":"En Mann","type":"Writer"},{"name":"Ei dame","type":"Writer"}],"processors":[],"rightsholders":[]},"metaImage":[{"altText":"Er bor det mange dyr","imageId":"42831","language":"nn"},{"altText":"Du er en apekatt","imageId":"42831","language":"nb"}],"published":"2017-08-21T21:30:16Z","updatedBy":"EnKulId","articleType":"standard","introduction":[{"language":"nn","introduction":"\nEn ny intro jadda"},{"language":"nb","introduction":"\nNy intro igjen jadda"}],"visualElement":[],"metaDescription":[{"content":"En lang meta beskrivelse","language":"nn"},{"content":"En kort metabeskrivelse","language":"nb"}],"requiredLibraries":[]}"""
    val expected2 =
      """{"tags":[{"tags":["fine","tags"],"language":"nn"},{"tags":["super","mann"],"language":"nb"}],"notes":[],"title":[{"title":"Noreg er et land","language":"nn"},{"title":"Norge er et land","language":"nb"}],"status":{"other":["IMPORTED"],"current":"PUBLISHED"},"content":[{"content":"<section><h1>Uten h5p</h1>Dette er en artikkel uten noe h5p</section>","language":"nn"}],"created":"2017-03-24T13:30:10Z","updated":"2017-08-21T21:30:16Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[{"name":"En Mann","type":"Writer"},{"name":"Ei dame","type":"Writer"}],"processors":[],"rightsholders":[]},"metaImage":[{"altText":"Er bor det mange dyr","imageId":"42831","language":"nn"},{"altText":"Du er en apekatt","imageId":"42831","language":"nb"}],"published":"2017-08-21T21:30:16Z","updatedBy":"EnKulId","articleType":"standard","introduction":[{"language":"nn","introduction":"\nEn ny intro jadda"},{"language":"nb","introduction":"\nNy intro igjen jadda"}],"visualElement":[],"metaDescription":[{"content":"En lang meta beskrivelse","language":"nn"},{"content":"En kort metabeskrivelse","language":"nb"}],"requiredLibraries":[]}"""

    val result1 = migration.convertArticleUpdate(old1)
    result1 should be(expected1)
    val result2 = migration.convertArticleUpdate(old2)
    result2 should be(expected2)
  }
}
