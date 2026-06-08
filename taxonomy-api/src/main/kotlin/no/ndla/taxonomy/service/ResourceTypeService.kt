/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.persistence.EntityManager
import no.ndla.taxonomy.domain.ResourceType
import no.ndla.taxonomy.repositories.ResourceTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ResourceTypeService(
    private val entityManager: EntityManager,
    private val resourceTypeRepository: ResourceTypeRepository,
) {

  @Transactional
  fun shiftOrderAfterInsertUpdate(resourceType: ResourceType) {
    if (resourceType.order == -1) {
      resourceType.order = resourceTypeRepository.nextOrderValue()
      entityManager.merge(resourceType)
    }

    var duplicate = false
    resourceTypeRepository.findAllByOrderByOrderAsc().forEachIndexed { i, rt ->
      if (rt.publicId != resourceType.publicId) {
        if (rt.order == resourceType.order) {
          duplicate = true
          rt.order = i + 1
        } else {
          rt.order = i + if (duplicate) 1 else 0
        }
        entityManager.merge(rt)
      }
    }
  }

  @Transactional
  fun updateOrderAfterDelete() {
    resourceTypeRepository.findAllByOrderByOrderAsc().forEachIndexed { i, rt ->
      rt.order = i
      entityManager.merge(rt)
    }
  }
}
