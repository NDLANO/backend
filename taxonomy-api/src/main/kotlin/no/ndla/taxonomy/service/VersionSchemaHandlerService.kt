/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceUnit
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class VersionSchemaHandlerService {

  @PersistenceUnit private lateinit var entityManagerFactory: EntityManagerFactory

  fun bindSession() {
    if (!TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
      val entityManager = entityManagerFactory.createEntityManager()
      TransactionSynchronizationManager.bindResource(
          entityManagerFactory, EntityManagerHolder(entityManager))
    }
  }

  fun unbindSession() {
    val emHolder =
        TransactionSynchronizationManager.unbindResource(entityManagerFactory)
            as EntityManagerHolder
    EntityManagerFactoryUtils.closeEntityManager(emHolder.entityManager)
  }
}
