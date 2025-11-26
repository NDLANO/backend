/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends MockitoSugar {
  given props: TestProps = TestProps()
  val dbUtil: DBUtility  = DBUtility()
}
