/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Resolves version hashes to database schema names, caching lookups for a short TTL so the version
 * table is not queried on every request.
 */
@Component
class VersionResolver(
    private val versionRepository: VersionRepository,
    @Value("\${taxonomy.version-schema-cache-ttl-seconds:30}") ttlSeconds: Long,
    @Value("\${spring.datasource.hikari.schema:taxonomy_api}") private val defaultSchema: String,
) {

  private val ttlNanos = Duration.ofSeconds(ttlSeconds).toNanos()

  private data class CachedSchema(val schema: String?, val expiresAtNanos: Long)

  private val publishedSchema = AtomicReference<CachedSchema?>()
  private val schemaByHash = ConcurrentHashMap<String, CachedSchema>()

  fun schemaFromHash(hash: String?): String = hash?.let { "${defaultSchema}_$it" } ?: defaultSchema

  /** Schema of the currently published version, or null if no version is published. */
  fun publishedVersionSchema(): String? {
    publishedSchema
        .get()
        ?.takeIf { it.expiresAtNanos > System.nanoTime() }
        ?.let {
          return it.schema
        }
    val schema =
        versionRepository.findFirstByVersionType(VersionType.PUBLISHED)?.let {
          schemaFromHash(it.hash)
        }
    publishedSchema.set(CachedSchema(schema, System.nanoTime() + ttlNanos))
    return schema
  }

  /** Schema of the version with the given hash, or null if no such version exists. */
  fun versionSchemaForHash(hash: String): String? {
    schemaByHash[hash]
        ?.takeIf { it.expiresAtNanos > System.nanoTime() }
        ?.let {
          return it.schema
        }
    // Hashes come from a client-supplied header, so bound the map to avoid unbounded growth
    if (schemaByHash.size >= MAX_HASH_ENTRIES) schemaByHash.clear()
    val schema = versionRepository.findFirstByHash(hash)?.let { schemaFromHash(it.hash) }
    schemaByHash[hash] = CachedSchema(schema, System.nanoTime() + ttlNanos)
    return schema
  }

  /**
   * Mutations to the versions should call this, but since we scale horizontally, the TTL still
   * needs to be quite short to keep all the pods in sync after a publication
   */
  fun invalidate() {
    publishedSchema.set(null)
    schemaByHash.clear()
  }

  companion object {
    private const val MAX_HASH_ENTRIES = 1000
  }
}
