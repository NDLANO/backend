/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.time.Instant

interface EntityWithMetadata {
  val metadata: Metadata

  fun setMetadata(metadata: Metadata, changeOwner: Boolean) {
    setCustomFields(metadata.getCustomFields())
    setGrepCodes(metadata.getGrepCodes())
    setVisible(metadata.isVisible())
    setUpdatedAt(metadata.getUpdatedAt())
    setCreatedAt(metadata.getCreatedAt())
    if (changeOwner) {
      metadata.setParent(this)
    }
  }

  fun setMetadata(metadata: Metadata) {
    this.setMetadata(metadata, true)
  }

  fun getGrepCodes(): Set<JsonGrepCode>

  fun isVisible(): Boolean

  fun getCreatedAt(): Instant?

  fun getUpdatedAt(): Instant?

  fun getCustomFields(): Map<String, String>

  fun setCustomField(key: String, value: String)

  fun unsetCustomField(key: String)

  fun setGrepCodes(codes: Set<JsonGrepCode>)

  fun setCustomFields(customFields: Map<String, String>)

  fun setVisible(visible: Boolean)

  fun setUpdatedAt(updatedAt: Instant?)

  fun setCreatedAt(createdAt: Instant?)
}
