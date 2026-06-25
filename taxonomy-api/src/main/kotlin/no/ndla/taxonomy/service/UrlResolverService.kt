/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import java.net.URI
import java.util.Optional
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.UrlMapping
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.repositories.UrlMappingRepository
import no.ndla.taxonomy.service.dtos.ResolvedUrl
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException
import no.ndla.taxonomy.util.PrettyUrlUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UrlResolverService(
    private val urlMappingRepository: UrlMappingRepository,
    private val nodeRepository: NodeRepository,
    private val canonifier: OldUrlCanonifier,
) {

  class NodeIdNotFoundException(message: String) : Exception(message)

  /**
   * @param oldUrl url previously imported into taxonomy with taxonomy-import
   * @return return a resolved URL or null
   */
  fun resolveOldUrl(oldUrl: String): String? {
    val result = getCachedUrlOldRig(oldUrl).firstOrNull() ?: return null
    result.subjectId?.let { subjectId ->
      findShortestPathStartingWith(subjectId, getAllPaths(result.publicId))?.let {
        return it
      }
    }
    return getPrimaryPath(result.publicId)
  }

  private fun findShortestPathStartingWith(subjectId: URI, allPaths: List<String>): String? =
      allPaths
          .filter { it.startsWith("/${subjectId.schemeSpecificPart}") }
          .minByOrNull { it.length }

  private fun getAllPaths(publicId: URI): List<String> =
      try {
        getEntityFromPublicId(publicId)?.allPaths?.toList() ?: emptyList()
      } catch (_: InvalidArgumentServiceException) {
        emptyList()
      }

  private fun getPrimaryPath(publicId: URI): String? =
      try {
        getEntityFromPublicId(publicId)?.primaryPath?.orElse(null)
      } catch (_: InvalidArgumentServiceException) {
        null
      }

  private fun getCachedUrlOldRig(oldUrl: String): List<UrlMapping> {
    val canonicalUrl = canonifier.canonify(oldUrl)
    val nodeId = getNodeId(canonicalUrl)
    return urlMappingRepository.findAllByOldUrlLike("$canonicalUrl%").filter { mapping ->
      // the LIKE query may match node IDs that __start with__ the same node ID as in old
      // url
      // e.g. oldUrl /node/54 should not match /node/54321 - therefore we add only if IDs
      // match
      getNodeId(mapping.oldUrl) == nodeId
    }
  }

  private fun getNodeId(url: String): String? =
      when {
        url.contains('?') && url.contains('/') ->
            url.substring(url.lastIndexOf('/'), url.indexOf('?'))

        url.contains('/') -> url.substring(url.lastIndexOf('/'))
        else -> null
      }

  /**
   * put old url into URL_MAP
   *
   * @param oldUrl url to put
   * @param nodeId nodeID to be associated with this URL
   * @param subjectId subjectID to be associated with this URL (optional)
   * @throws NodeIdNotFoundException if node id not found in taxonomy
   */
  @Transactional
  @Throws(NodeIdNotFoundException::class)
  fun putUrlMapping(oldUrl: String, nodeId: URI, subjectId: URI? = null) {
    val canonified = canonifier.canonify(oldUrl)
    if (getAllPaths(nodeId).isEmpty()) {
      throw NodeIdNotFoundException("Node id not found in taxonomy for $canonified")
    }
    val existing = urlMappingRepository.findAllByOldUrl(canonified)
    if (existing.isEmpty()) {
      val urlMapping = UrlMapping(oldUrl = canonified, publicId = nodeId, subjectId = subjectId)
      urlMappingRepository.save(urlMapping)
    } else {
      existing.forEach { mapping ->
        mapping.publicId = nodeId
        mapping.subjectId = subjectId
      }
    }
  }

  fun resolveUrl(path: String, language: String): ResolvedUrl? =
      try {
        val resolvedPathComponents = resolveEntitiesFromPath(path)
        val normalizedPath = resolvedPathComponents.joinToString("") { n -> n.pathPart }
        val leafNode = resolvedPathComponents.last()
        val context = leafNode.contexts.firstOrNull { it.path() == normalizedPath }

        val ctx =
            context
                ?: leafNode
                    .pickContext(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        NodeConnectionType.BRANCH,
                        setOf(),
                    )
                    .orElse(null)
                ?: throw NotFoundServiceException("No context found for path")

        val prettyUrl =
            PrettyUrlUtil.createPrettyUrl(
                ctx.rootName,
                ctx.name,
                language,
                ctx.contextId,
                ctx.nodeType,
            )

        ResolvedUrl(
            exactMatch = context != null,
            contentUri = leafNode.contentUri,
            id = URI.create(ctx.publicId),
            parents = ctx.parentIds.map(URI::create).reversed(),
            name = ctx.name.fromLanguage(language),
            path = ctx.path,
            url = prettyUrl ?: ctx.path ?: "",
        )
      } catch (_: Exception) {
        null
      }

  private fun resolveEntitiesFromPath(path: String): List<Node> =
      path
          .split(Regex("/+"))
          .filter { it.isNotEmpty() }
          .mapNotNull {
            try {
              getEntityFromPublicId(URI.create("urn:$it"))
            } catch (_: Exception) {
              // Do nothing, just skip the part of the path that could not be resolved
              null
            }
          }

  private fun getEntityFromPublicId(publicId: URI): Node? {
    if (publicId.scheme != "urn") {
      throw InvalidArgumentServiceException("No valid URN provided")
    }
    return nodeRepository.findFirstByPublicId(publicId).orElse(null)
  }
}
