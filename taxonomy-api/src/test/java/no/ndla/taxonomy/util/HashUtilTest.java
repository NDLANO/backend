/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HashUtilTest {

    @Test
    public void get_different_length_hashes() {
        String shortHash = HashUtil.shortHash("original");
        String mediumHash = HashUtil.mediumHash("original");
        String semiHash = HashUtil.semiHash("original");
        String longHash = HashUtil.longHash("original");
        String fullHash = HashUtil.fullHash("original");

        assertEquals(4, shortHash.length());
        assertEquals(10, mediumHash.length());
        assertEquals(12, semiHash.length());
        assertEquals(16, longHash.length());
        assertTrue(fullHash.length() > 16);
    }

    @Test
    public void same_string_gives_same_hashes() {
        String hash1 = HashUtil.mediumHash("original");
        String hash2 = HashUtil.mediumHash("original");
        String hash3 = HashUtil.longHash("original");
        assertEquals(hash1, hash2);
        assertTrue(hash3.startsWith(hash2));
    }
}
