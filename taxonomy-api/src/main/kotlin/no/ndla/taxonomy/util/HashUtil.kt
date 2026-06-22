/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util

import org.apache.commons.codec.digest.DigestUtils

/** Util for generating hashes in different lengths */
object HashUtil {
  private fun generateHash(original: Any, length: Int): String {
    val hash = DigestUtils.sha3_256Hex(original.toString())
    return if (length > 0) hash.substring(0, length) else hash
  }

  fun shortHash(original: Any) = generateHash(original, 4)

  fun mediumHash(original: Any) = generateHash(original, 10)

  fun semiHash(original: Any) = generateHash(original, 12)

  fun longHash(original: Any) = generateHash(original, 16)

  fun fullHash(original: Any) = generateHash(original, 0)
}
