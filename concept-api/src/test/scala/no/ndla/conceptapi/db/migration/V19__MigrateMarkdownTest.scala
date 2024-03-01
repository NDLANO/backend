/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V19__MigrateMarkdownTest extends UnitSuite with TestEnvironment {
  val migration = new V19__MigrateMarkdown

  test("That markdown is migrated correctly") {
    val original =
      """{"tags":[],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier *noe* om hvor __mange__ ganger vi puster i løpet av ett ~minutt~. Ved ^høyere aktivitetsnivå^ øker lungefrekvensen fordi **kroppen** trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""
    val expected =
      """{"tags":[],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier <em>noe</em> om hvor <strong>mange</strong> ganger vi puster i løpet av ett <sub>minutt</sub>. Ved <sup>høyere aktivitetsnivå</sup> øker lungefrekvensen fordi <strong>kroppen</strong> trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""

    val result = migration.convertToNewConcept(original)
    result should be(expected)
  }

}
