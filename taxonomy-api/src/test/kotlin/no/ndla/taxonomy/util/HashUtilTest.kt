/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HashUtilTest {

  @Test
  fun get_different_length_hashes() {
    val shortHash = HashUtil.shortHash("original")
    val mediumHash = HashUtil.mediumHash("original")
    val semiHash = HashUtil.semiHash("original")
    val longHash = HashUtil.longHash("original")
    val fullHash = HashUtil.fullHash("original")

    assertEquals(4, shortHash.length)
    assertEquals(10, mediumHash.length)
    assertEquals(12, semiHash.length)
    assertEquals(16, longHash.length)
    assertTrue(fullHash.length > 16)
  }

  @Test
  fun same_string_gives_same_hashes() {
    val hash1 = HashUtil.mediumHash("original")
    val hash2 = HashUtil.mediumHash("original")
    val hash3 = HashUtil.longHash("original")
    assertEquals(hash1, hash2)
    assertTrue(hash3.startsWith(hash2))
  }
}
