/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

/** Contains a ThreadLocal object to store a database schema name for use in Connection objects. */
object VersionContext {
  private val currentVersion = ThreadLocal<String?>()

  @JvmStatic fun setCurrentVersion(tenant: String) = currentVersion.set(tenant)

  @JvmStatic fun getCurrentVersion(): String? = currentVersion.get()

  fun clear() = currentVersion.remove()
}
