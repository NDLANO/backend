/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import java.net.URI
import java.time.Instant
import java.util.concurrent.Executors
import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import no.ndla.taxonomy.rest.v1.commands.VersionPost
import no.ndla.taxonomy.service.dtos.VersionDTO
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException
import no.ndla.taxonomy.service.task.Deleter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class VersionService(
    private val entityManager: EntityManager,
    private val versionRepository: VersionRepository,
    private val nodeConnectionService: NodeConnectionService,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val validator = URNValidator()
  private val executor = Executors.newSingleThreadExecutor()

  @Value("\${spring.datasource.hikari.schema:taxonomy_api}")
  private lateinit var defaultSchema: String

  @Transactional
  fun delete(publicId: URI) {
    val versionToDelete =
        versionRepository.findFirstByPublicId(publicId)
            ?: throw NotFoundServiceException("Version was not found")

    val schema = schemaFromHash(versionToDelete.hash)
    try {
      entityManager
          .createNativeQuery(String.format("DROP SCHEMA %s CASCADE", schema))
          .executeUpdate()
    } catch (_: PersistenceException) {
      logger.warn("Failed to drop schema. Possible manual cleanup required")
    }
    versionRepository.delete(versionToDelete)
  }

  fun getVersions(): List<VersionDTO> = versionRepository.findAll().map(::VersionDTO)

  fun getVersionsOfType(versionType: VersionType) =
      versionRepository.findByVersionType(versionType).map(::VersionDTO)

  @Transactional
  @Async
  fun publishBetaAndArchiveCurrent(id: URI) {
    versionRepository.findFirstByVersionType(VersionType.PUBLISHED)?.let {
      it.versionType = VersionType.ARCHIVED
      // TODO: Replace this with Clock.System.now() at some point
      it.archived = Instant.now()
      versionRepository.saveAndFlush(it)
    }

    val beta = versionRepository.getByPublicId(id)
    beta.versionType = VersionType.PUBLISHED
    beta.isLocked = true
    // TODO: Replace this with Clock.System.now() at some point
    beta.published = Instant.now()
    versionRepository.saveAndFlush(beta)

    disconnectAllInvisibleNodes(beta.hash)
  }

  private fun disconnectAllInvisibleNodes(hash: String) {
    // Use a task to run in a separate thread against a specified schema
    // Do not care about the result so no need to wait for it
    try {
      val deleter = Deleter()
      deleter.setNodeConnectionService(nodeConnectionService)
      deleter.setVersion(schemaFromHash(hash))
      executor.submit(deleter).get()
    } catch (e: Exception) {
      logger.info(e.message, e)
    }
  }

  @Transactional
  fun createNewVersion(sourceId: URI?, command: VersionPost): Version {
    val entity = Version()
    command.id?.let {
      validator.validate(it, entity)
      entity.publicId = it
    }

    command.apply(entity)
    val version = versionRepository.save(entity)

    var sourceSchema = defaultSchema
    sourceId?.let {
      val source = versionRepository.getByPublicId(it)
      sourceSchema = schemaFromHash(source.hash)
    }

    val schema = schemaFromHash(version.hash)
    // JPA does not like functions returning void so adds a count(*) to sql.
    entityManager
        .createNativeQuery(
            "SELECT count(*) from clone_schema('$sourceSchema', '$schema', true, false)")
        .singleResult

    return version
  }

  fun schemaFromHash(hash: String?) = hash?.let { "${defaultSchema}_$it" } ?: defaultSchema
}
