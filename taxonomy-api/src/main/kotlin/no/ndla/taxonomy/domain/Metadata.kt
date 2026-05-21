/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.io.Serializable
import java.time.Instant
import java.util.Objects
import java.util.stream.Collectors
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT

class Metadata : Serializable {
  @JvmField protected var updatedAt: Instant? = null
  @JvmField protected var createdAt: Instant? = null
  @JvmField protected var grepCodes: MutableSet<JsonGrepCode> = HashSet()
  @JvmField protected var customFields: Map<String, String> = HashMap()
  @JvmField protected var visible: Boolean = true
  @JvmField protected var parent: EntityWithMetadata? = null

  constructor()

  constructor(parent: EntityWithMetadata) {
    this.parent = parent
    this.grepCodes = HashSet(parent.getGrepCodes())
    this.visible = parent.isVisible()
    this.createdAt = parent.getCreatedAt()
    this.updatedAt = parent.getUpdatedAt()
    this.customFields = parent.getCustomFields()
  }

  constructor(metadata: Metadata) {
    this.parent = metadata.parent
    this.createdAt = metadata.createdAt
    this.customFields = metadata.getCustomFields()
    this.grepCodes = HashSet(metadata.getGrepCodes())
    this.updatedAt = metadata.updatedAt
    this.visible = metadata.isVisible()
  }

  fun mergeWith(toMerge: MetadataPUT): Metadata {
    toMerge.visible.ifPresent { this.setVisible(it) }
    toMerge.grepCodes.ifPresent { this.setGrepCodes(it) }
    toMerge.customFields.ifPresent { this.setCustomFields(it) }
    return this
  }

  fun setParent(parent: EntityWithMetadata) {
    this.parent = parent
  }

  fun addGrepCode(grepCode: JsonGrepCode) {
    this.grepCodes.add(grepCode)
    this.parent!!.setGrepCodes(this.grepCodes)
  }

  fun setGrepCodes(grepCodes: Set<String>) {
    val newJsonGrepCodes = grepCodes.stream().map { JsonGrepCode(it) }.collect(Collectors.toSet())
    this.grepCodes = newJsonGrepCodes
    this.parent!!.setGrepCodes(newJsonGrepCodes)
  }

  fun setCustomFields(customFields: Map<String, String>) {
    this.customFields = customFields
    this.parent!!.setCustomFields(customFields)
  }

  fun removeGrepCode(grepCode: JsonGrepCode) {
    this.grepCodes.remove(grepCode)
    this.parent!!.setGrepCodes(this.grepCodes)
  }

  fun getGrepCodes(): Set<JsonGrepCode> = HashSet(this.grepCodes)

  fun getCustomFields(): Map<String, String> = this.customFields

  fun isVisible(): Boolean = this.visible

  fun getUpdatedAt(): Instant? = this.updatedAt

  fun getCreatedAt(): Instant? = this.createdAt

  fun setVisible(visible: Boolean) {
    this.visible = visible
    this.parent!!.setVisible(visible)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as Metadata
    return visible == that.visible &&
        Objects.equals(updatedAt, that.updatedAt) &&
        Objects.equals(createdAt, that.createdAt) &&
        Objects.equals(grepCodes, that.grepCodes) &&
        Objects.equals(customFields, that.customFields)
  }
}
