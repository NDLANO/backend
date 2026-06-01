/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Gets the database schema name from the version context to be used in the multi tenancy provider.
 */
@Component
class VersionIdentifierResolver : CurrentTenantIdentifierResolver<String> {

  @Value("\${spring.datasource.hikari.schema:taxonomy_api}")
  private lateinit var defaultSchema: String

  override fun resolveCurrentTenantIdentifier(): String =
      VersionContext.getCurrentVersion() ?: defaultSchema

  override fun validateExistingCurrentSessions(): Boolean = true
}
