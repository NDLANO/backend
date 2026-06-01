/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import com.fasterxml.jackson.annotation.JsonIgnore
import java.net.URI

interface UpdatableDto<T> {
  @get:JsonIgnore
  val id: URI?
    get() = null

  fun apply(entity: T)
}
