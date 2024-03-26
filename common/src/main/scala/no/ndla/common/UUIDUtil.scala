/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import java.util.UUID

trait UUIDUtil {
  val uuidUtil: UUIDUtil

  class UUIDUtil {
    def randomUUID(): UUID = {
      UUID.randomUUID()
    }
  }
}
