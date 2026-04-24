/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import org.springframework.data.jpa.domain.Specification;

public interface ExtraSpecification {
    Specification<Node> applySpecification(Specification<Node> spec);
}
