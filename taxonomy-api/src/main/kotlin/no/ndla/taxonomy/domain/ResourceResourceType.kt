/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreRemove
import jakarta.persistence.Transient
import java.net.URI
import java.util.UUID

@Entity
class ResourceResourceType : DomainEntity(), Comparable<ResourceResourceType> {

  @ManyToOne(cascade = [CascadeType.MERGE])
  @JoinColumn(name = "resource_id")
  lateinit var node: Node

  @ManyToOne(cascade = [CascadeType.MERGE])
  @JoinColumn(name = "resource_type_id")
  lateinit var resourceType: ResourceType

  @Transient private var disassociated = false

  init {
    publicId = URI.create("urn:resource-resourcetype:${UUID.randomUUID()}")
  }

  companion object {
    fun create(resource: Node, resourceType: ResourceType): ResourceResourceType =
        ResourceResourceType().also {
          it.node = resource
          it.resourceType = resourceType
          resource.addResourceResourceType(it)
        }
  }

  fun disassociate() {
    if (disassociated) return
    disassociated = true
    node.removeResourceResourceType(this)
  }

  @PreRemove fun preRemove() = disassociate()

  override fun compareTo(other: ResourceResourceType): Int {
    if (!::resourceType.isInitialized || !other::resourceType.isInitialized) return 0
    return resourceType.compareTo(other.resourceType)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ResourceResourceType) return false
    if (!::node.isInitialized || !other::node.isInitialized) return false
    if (!::resourceType.isInitialized || !other::resourceType.isInitialized) return false
    return node.publicId == other.node.publicId &&
        resourceType.publicId == other.resourceType.publicId
  }

  override fun hashCode(): Int {
    if (!::node.isInitialized || !::resourceType.isInitialized) return System.identityHashCode(this)
    return 31 * node.publicId.hashCode() + resourceType.publicId.hashCode()
  }
}
