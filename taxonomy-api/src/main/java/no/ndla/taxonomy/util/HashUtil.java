/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Util for generating hashes in different lengths
 */
public class HashUtil {
    private static String generateHash(Object original, int length) {
        String hash = new DigestUtils("SHA3-256").digestAsHex(original.toString());
        if (length > 0) return hash.substring(0, length);
        return hash;
    }

    public static String shortHash(Object original) {
        return generateHash(original, 4);
    }

    public static String mediumHash(Object original) {
        return generateHash(original, 10);
    }

    public static String semiHash(Object original) {
        return generateHash(original, 12);
    }

    public static String longHash(Object original) {
        return generateHash(original, 16);
    }

    public static String fullHash(Object original) {
        return generateHash(original, 0);
    }
}
