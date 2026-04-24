/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

/**
 * Contains a ThreadLocal object to store a database schema name for use in Connection objects.
 */
public class VersionContext {
    private static final ThreadLocal<String> currentVersion = new ThreadLocal<>();

    public static void setCurrentVersion(String tenant) {
        currentVersion.set(tenant);
    }

    public static String getCurrentVersion() {
        return currentVersion.get();
    }

    public static void clear() {
        currentVersion.remove();
    }
}
