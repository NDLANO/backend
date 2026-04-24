/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

public interface Translation {
    String getName();

    void setName(String name);

    String getLanguageCode();
}
