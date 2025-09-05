/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.learningpathapi.db.util.StepDocumentRow
import no.ndla.learningpathapi.db.util.LpDocumentRow

class V53__FixNegativeSeqNoTest extends UnitSuite with TestEnvironment {

  test("That negative seqNo are converted to non-negative seqNo") {
    val migration = new V53__FixNegativeSeqNo

    val lpData = LpDocumentRow(1, "")

    val stepDatas = List(
      StepDocumentRow(1, """{"seqNo":-2}"""),
      StepDocumentRow(2, """{"seqNo":-1}"""),
      StepDocumentRow(3, """{"seqNo":0}"""),
      StepDocumentRow(4, """{"seqNo":1}""")
    )
    val expectedStepDatas = List(
      StepDocumentRow(1, """{"seqNo":0}"""),
      StepDocumentRow(2, """{"seqNo":1}"""),
      StepDocumentRow(3, """{"seqNo":2}"""),
      StepDocumentRow(4, """{"seqNo":3}""")
    )
    migration.convertPathAndSteps(lpData, stepDatas) should be((lpData, expectedStepDatas))
  }
}
