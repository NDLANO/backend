/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import no.ndla.common.configuration.BaseProps

class TestProps extends BaseProps, DatabaseProps {
  override def ApplicationName: String = "database"

  // These are unused, but are required to implement `BaseProps` and `DatabaseProps`
  override def ApplicationPort: Int          = ???
  override def MetaMigrationLocation: String = ???
}
