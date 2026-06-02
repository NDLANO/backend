/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.servlet.http.HttpServletRequest
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Takes an http request and extracts a given header. Returns a database schema name. */
@Component
class VersionHeaderExtractor(
    private val versionService: VersionService,
    private val versionRepository: VersionRepository
) {

  @Value("\${spring.datasource.hikari.schema:taxonomy_api}")
  private lateinit var defaultSchema: String

  fun getVersionSchemaFromHeader(req: HttpServletRequest): String {
    if (req.requestURI.startsWith("/v1/versions")) {
      return defaultSchema
    }
    return try {
      val versionHash = req.getHeader("VersionHash")
      when {
        // Header supplied, use that version if in database. Else use default.
        versionHash != null ->
            versionRepository.findFirstByHash(versionHash)?.let {
              versionService.schemaFromHash(it.hash)
            } ?: defaultSchema

        // No header, check published and use for gets
        req.method == "GET" ->
            versionRepository.findFirstByVersionType(VersionType.PUBLISHED)?.let {
              versionService.schemaFromHash(it.hash)
            } ?: defaultSchema

        else -> defaultSchema
      }
    } catch (_: Exception) {
      // Something happened when fetching version!
      return defaultSchema
    }
  }
}
