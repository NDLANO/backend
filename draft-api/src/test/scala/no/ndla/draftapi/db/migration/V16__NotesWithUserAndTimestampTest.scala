/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import no.ndla.draftapi.db.migration.V16__NotesWithUserAndTimestamp
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V16__NotesWithUserAndTimestampTest extends UnitSuite with TestEnvironment {
  val migration = new V16__NotesWithUserAndTimestamp
  test("migration should update to new note format") {
    val old =
      s"""{
        | "updatedBy": "User1",
        | "created": "2018-08-07T13:05:10Z",
        | "updated": "2018-08-07T13:05:15Z"
        | "notes": [ "Notat1", "Notat2" ],
        | "status": {
        |   "other": [],
        |   "current": "IN_PROGRESS"
        | }
        |}""".stripMargin

    val expected =
      s"""{"updatedBy":"User1","created":"2018-08-07T13:05:10Z","updated":"2018-08-07T13:05:15Z","notes":[{"note":"Notat1","user":"System","status":{"current":"IN_PROGRESS","other":[]},"timestamp":"2018-08-07T13:05:15Z"},{"note":"Notat2","user":"System","status":{"current":"IN_PROGRESS","other":[]},"timestamp":"2018-08-07T13:05:15Z"}],"status":{"other":[],"current":"IN_PROGRESS"}}"""
    migration.convertNotes(old) should be(expected)
  }

  test("migration not do anything if the document already has new note format") {
    val old =
      s"""{"updatedBy":"User1","created":"2018-08-07T13:05:10Z","updated":"2018-08-07T13:05:15Z","notes":[{"note":"Notat1","user":"User1","status":{"current":"IN_PROGRESS","other":[]},"timestamp":"2018-08-07T13:05:15Z"}],"status":{"other":[],"current":"IN_PROGRESS"}}"""

    migration.convertNotes(old) should be(old)
  }
}
