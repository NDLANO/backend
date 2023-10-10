/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V17__MoveSourceToOriginTest extends UnitSuite with TestEnvironment {
  val migration = new V17__MoveSourceToOrigin

  test("That empty source is moved to missing origin copyright") {
    val original =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"source":"","status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""
    val expected =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""

    val result = migration.convertDocument(original)
    result should be(expected)
  }

  test("That plain source is moved to missing origin copyright") {
    val original =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"source":"https://example.com/kulkilde","status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""
    val expected =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[],"origin":"https://example.com/kulkilde"},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""

    val result = migration.convertDocument(original)
    result should be(expected)
  }

  test("That markdown sources are extracted to plain urls") {
    val original =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"source":"[Kul kilde](https://example.com/kulkilde)","status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[]},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""
    val expected =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[],"origin":"https://example.com/kulkilde"},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""

    val result = migration.convertDocument(original)
    result should be(expected)

  }

  test("That origin is overwritten if empty-string") {
    val original =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"source":"[Kul kilde](https://example.com/kulkilde)","status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[],"origin":""},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""
    val expected =
      """{"tags":[{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nb"},{"tags":["lunge","lungefrekvens","fysisk aktivitet","trening","kroppsøving"],"language":"nn"}],"title":[{"title":"lungefrekvens","language":"nb"},{"title":"lungefrekvens","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"IN_PROGRESS"},"content":[{"content":"Lungefrekvensen sier noe om hvor mange ganger vi puster i løpet av ett minutt. Ved høyere aktivitetsnivå øker lungefrekvensen fordi kroppen trenger mer oksygen under aktivitet enn i hvile. ","language":"nb"},{"content":"Lungefrekvensen seier noko om kor mange gonger vi pustar i løpet av eitt minutt. Ved høgare aktivitetsnivå aukar lungefrekvensen fordi kroppen treng meir oksygen under aktivitet enn i kvile. ","language":"nn"}],"created":"2019-11-13T10:20:43Z","updated":"2020-07-09T10:19:49Z","copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"Cathrine Dunker Furuly","type":"Writer"}],"processed":false,"processors":[],"rightsholders":[],"origin":"https://example.com/kulkilde"},"metaImage":[],"updatedBy":["sPHJn0BEtfxw2d2DUpIuS3iY"],"articleIds":[],"subjectIds":[],"conceptType":"concept","visualElement":[],"supportedLanguages":null}"""

    val result = migration.convertDocument(original)
    result should be(expected)

  }

}
