/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicBoolean;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResourceTypeService {
    private final EntityManager entityManager;
    private final ResourceTypeRepository resourceTypeRepository;

    public ResourceTypeService(EntityManager entityManager, ResourceTypeRepository resourceTypeRepository) {
        this.entityManager = entityManager;
        this.resourceTypeRepository = resourceTypeRepository;
    }

    @Transactional
    public void shiftOrderAfterInsertUpdate(ResourceType resourceType) {
        if (resourceType.getOrder() == -1) {
            resourceType.setOrder(resourceTypeRepository.nextOrderValue());
            entityManager.merge(resourceType);
        }
        var allResourceTypes = resourceTypeRepository.findAllByOrderByOrderAsc();
        var duplicate = new AtomicBoolean(false);
        for (var i = 0; i < allResourceTypes.size(); i++) {
            var rt = allResourceTypes.get(i);
            if (!rt.getPublicId().equals(resourceType.getPublicId())) {
                if (rt.getOrder() == resourceType.getOrder()) {
                    duplicate.set(true);
                    rt.setOrder(i + 1);
                } else {
                    rt.setOrder(i + (duplicate.get() ? 1 : 0));
                }
                entityManager.merge(rt);
            }
        }
    }

    @Transactional
    public void updateOrderAfterDelete() {
        var allResourceTypes = resourceTypeRepository.findAllByOrderByOrderAsc();
        for (var i = 0; i < allResourceTypes.size(); i++) {
            var rt = allResourceTypes.get(i);
            rt.setOrder(i);
            entityManager.merge(rt);
        }
    }
}
