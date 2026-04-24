/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Node;

public interface DomainEntityHelperService {
    Node getNodeByPublicId(URI publicId);

    DomainEntity getEntityByPublicId(URI publicId);
}
