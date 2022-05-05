/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package audioapi.db.migration

import audioapi.db.migration.{V4_Audio, V4_AudioMetaInformation, V4_Copyright, V4_Tag, V4_Title, V4__AddLanguageToAll}

import java.util.Date
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V4__AddLanguageToAllTest extends UnitSuite with TestEnvironment {

  val migration = new V4__AddLanguageToAll

  test("add language to stuff with missing language") {
    val tags      = Seq(V4_Tag(Seq(), Some("en")), V4_Tag(Seq(), Some("")))
    val filePaths = Seq(V4_Audio("", "", 0, Some("")), V4_Audio("", "", 0, Some("nb")))
    val titles    = Seq(V4_Title("En tittel", None), V4_Title("abc", Some("nb")))

    val before =
      V4_AudioMetaInformation(Some(1), Some(1), titles, filePaths, V4_Copyright("", None, Seq()), tags, "", new Date())
    val after = migration.convertAudioUpdate(before)

    after.titles.head.language should equal(Some("und"))
    after.titles.last.language should equal(Some("nb"))
    after.filePaths.head.language should equal(Some("und"))
    after.filePaths.last.language should equal(Some("nb"))
    after.tags.head.language should equal(Some("en"))
    after.tags.last.language should equal(Some("und"))
  }
}
