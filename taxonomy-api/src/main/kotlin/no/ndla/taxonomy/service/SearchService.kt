/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import java.net.URI
import java.util.Optional
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.service.dtos.NodeDTO
import no.ndla.taxonomy.service.dtos.SearchResultDTO
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

@Service
class SearchService(val nodeRepository: NodeRepository) {

  private fun uriList(ids: List<String>?): List<URI>? =
      ids?.mapNotNull { runCatching { URI(it) }.getOrNull() }?.takeIf { it.isNotEmpty() }

  private fun Specification<Node>.withQuery(query: String?) =
      query?.let { and { r, _, cb -> cb.like(cb.lower(r.get("name")), "%${it.lowercase()}%") } }
          ?: this

  private fun Specification<Node>.withIds(ids: List<String>?) =
      uriList(ids)?.let { and { r, _, _ -> r.get<URI>("publicId").`in`(it) } } ?: this

  private fun Specification<Node>.withContentUris(contentUris: List<String>?) =
      uriList(contentUris)?.let { and { r, _, _ -> r.get<URI>("contentUri").`in`(it) } } ?: this

  private fun Specification<Node>.withNodeType(nodeType: List<NodeType>?) =
      nodeType?.let { and { r, _, _ -> r.get<NodeType>("nodeType").`in`(it) } } ?: this

  private fun Specification<Node>.withCustomFields(filters: Map<String, String>?) =
      filters
          ?.entries
          ?.fold(null as Specification<Node>?) { acc, (k, v) ->
            val cond =
                Specification<Node> { r, _, cb ->
                  cb.equal(
                      cb.function(
                          "jsonb_extract_path_text",
                          String::class.java,
                          r.get<Map<String, String>>("customfields"),
                          cb.literal(k),
                      ),
                      v,
                  )
                }
            acc?.or(cond) ?: cond
          }
          ?.let { and(it) } ?: this

  fun search(
      query: String? = null,
      ids: List<String>? = null,
      contentUris: List<String>? = null,
      language: String,
      includeContexts: Boolean,
      filterProgrammes: Boolean,
      pageSize: Int,
      page: Int,
      nodeType: List<NodeType>? = null,
      customFieldFilters: Map<String, String>? = null,
      rootId: URI? = null,
      parentId: URI? = null,
  ): SearchResultDTO<NodeDTO> {
    if (page < 1) throw IllegalArgumentException("page parameter must be bigger than 0")

    val pageRequest = PageRequest.of(page - 1, pageSize)

    val spec =
        Specification<Node> { r, _, cb -> cb.isNotNull(r.get<String>("id")) }
            .withQuery(query)
            .withIds(ids)
            .withContentUris(contentUris)
            .withNodeType(nodeType)
            .withCustomFields(customFieldFilters)

    val fetched = nodeRepository.findAll(spec, pageRequest)

    val rootNode = rootId?.let(nodeRepository::findFirstByPublicId) ?: Optional.empty()
    val parentNode = parentId?.let(nodeRepository::findFirstByPublicId) ?: Optional.empty()

    val dtos =
        fetched.content.map { r ->
          NodeDTO(
              rootNode,
              parentNode,
              r,
              NodeConnectionType.BRANCH,
              language,
              Optional.empty(),
              includeContexts,
              filterProgrammes,
              false,
              false,
          )
        }

    return SearchResultDTO(fetched.totalElements, page, pageSize, dtos)
  }
}
