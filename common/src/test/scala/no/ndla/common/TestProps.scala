/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.configuration.BaseProps
import no.ndla.database.DatabaseProps

class TestProps extends BaseProps, DatabaseProps {
  override def ApplicationName: String = "common"

  // These are unused, but are required to implement `BaseProps` and `DatabaseProps`
  override def ApplicationPort: Int          = ???
  override def MetaMigrationLocation: String = ???
}
