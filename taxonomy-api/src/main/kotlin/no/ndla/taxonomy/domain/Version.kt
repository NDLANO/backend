/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.net.URI
import java.time.Instant
import java.util.UUID
import no.ndla.taxonomy.util.HashUtil

@Entity
class Version : DomainEntity() {

  @Column @Enumerated(EnumType.STRING) var versionType: VersionType = VersionType.BETA

  @Column var name: String? = null

  @Column lateinit var hash: String

  @Column(name = "locked") var isLocked: Boolean = false

  @Column var created: Instant = Instant.now()

  @Column var published: Instant? = null

  @Column var archived: Instant? = null

  init {
    publicId = URI.create("urn:version:${UUID.randomUUID()}")
    hash = HashUtil.shortHash(publicId)
  }
}
