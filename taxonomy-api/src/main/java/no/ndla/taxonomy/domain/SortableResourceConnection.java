/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.Optional;

public interface SortableResourceConnection {
    Optional<Node> getResource();

    Optional<Node> getParent();

    int getRank();
}
