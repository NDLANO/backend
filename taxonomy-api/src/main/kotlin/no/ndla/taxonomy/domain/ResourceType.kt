/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.net.URI
import java.util.UUID
import org.hibernate.annotations.Type

@Entity
class ResourceType : DomainObject(), Comparable<ResourceType> {

  init {
    publicId = URI.create("urn:resourcetype:${UUID.randomUUID()}")
  }

  @field:ManyToOne @field:JoinColumn(name = "parent_id") var parent: ResourceType? = null

  @field:Type(JsonBinaryType::class)
  @field:Column(name = "translations", columnDefinition = "jsonb")
  override var translations: MutableList<JsonTranslation> = ArrayList()

  @field:Column var order: Int = -1

  override fun compareTo(other: ResourceType): Int {
    if (order == -1 || other.order == -1) return publicId.compareTo(other.publicId)
    return order.compareTo(other.order)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ResourceType) return false
    return publicId == other.publicId
  }

  override fun hashCode() = publicId.hashCode()
}
