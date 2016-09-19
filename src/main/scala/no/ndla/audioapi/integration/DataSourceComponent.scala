/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import javax.sql.DataSource

trait DataSourceComponent {
  val dataSource: DataSource
}
