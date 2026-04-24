/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.exceptions.IdFormatException;

public class URNValidator {
    public void validate(URI id, DomainEntity entity) {
        final String[] idParts = id.toString().split(":");
        if (!"urn".equals(idParts[0])) {
            throw new IdFormatException("Id should start with urn:");
        }
        if (!idParts[1].equals(entity.getEntityName())) {
            throw new IdFormatException("Id should contain entity name");
        }
        if (idParts.length < 3) {
            throw new IdFormatException("Id should have id field");
        }
    }
}
