/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;

public interface ContextUpdaterService {
    void updateContexts(Node entity);

    void clearContexts(Node entity);
}
