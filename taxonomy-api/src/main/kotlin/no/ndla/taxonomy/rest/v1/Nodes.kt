/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.NodeConnectionRepository
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.commands.NodePostPut
import no.ndla.taxonomy.rest.v1.commands.NodeSearchBody
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.ContextUpdaterService
import no.ndla.taxonomy.service.MetadataFilters
import no.ndla.taxonomy.service.NodeService
import no.ndla.taxonomy.service.QualityEvaluationService
import no.ndla.taxonomy.service.RecursiveNodeTreeService
import no.ndla.taxonomy.service.SearchService
import no.ndla.taxonomy.service.TreeSorter
import no.ndla.taxonomy.service.dtos.ConnectionDTO
import no.ndla.taxonomy.service.dtos.MetadataDTO
import no.ndla.taxonomy.service.dtos.NodeChildDTO
import no.ndla.taxonomy.service.dtos.NodeDTO
import no.ndla.taxonomy.service.dtos.NodeWithParents
import no.ndla.taxonomy.service.dtos.SearchResultDTO
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.Optional

@RestController
@RequestMapping(path = ["/v1/nodes", "/v1/nodes/"])
class Nodes(
    private val nodeRepository: NodeRepository,
    private val nodeConnectionRepository: NodeConnectionRepository,
    private val nodeService: NodeService,
    private val contextUpdaterService: ContextUpdaterService,
    private val recursiveNodeTreeService: RecursiveNodeTreeService,
    private val treeSorter: TreeSorter,
    private val qualityEvaluationService: QualityEvaluationService,
    private val searchService: SearchService,
) {

    private val location: String by lazy { controllerLocation(javaClass) }

    private fun getDefaultNodeTypes(
        nodeType: Optional<List<NodeType>>,
        contentURI: Optional<URI>,
        contextId: Optional<String>,
        isContext: Optional<Boolean>,
        metadataFilters: MetadataFilters,
    ): List<NodeType> {
        if (nodeType.isPresent && nodeType.get().isNotEmpty()) {
            return nodeType.get()
        }
        if (contentURI.isEmpty && contextId.isEmpty && isContext.isEmpty && !metadataFilters.hasFilters()) {
            return listOf(NodeType.TOPIC, NodeType.NODE, NodeType.SUBJECT, NodeType.PROGRAMME)
        }
        return NodeType.entries.toList()
    }

    @GetMapping
    @Operation(summary = "Gets all nodes")
    @Transactional(readOnly = true)
    fun getAllNodes(
        @Parameter(
            description = "Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^)",
        )
        @RequestParam(value = "nodeType", required = false)
        nodeType: Optional<List<NodeType>>,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "Filter by contentUri")
        @RequestParam(value = "contentURI", required = false)
        contentUri: Optional<URI>,
        @Parameter(description = "Ids to filter by")
        @RequestParam(value = "ids", required = false)
        publicIds: Optional<List<URI>>,
        @Parameter(description = "Only root level contexts", deprecated = true)
        @RequestParam(value = "isRoot", required = false)
        isRoot: Optional<Boolean>,
        @Parameter(description = "Only contexts")
        @RequestParam(value = "isContext", required = false)
        isContext: Optional<Boolean>,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "key", required = false)
        key: Optional<String>,
        @Parameter(description = "Filter by key and value")
        @RequestParam(value = "value", required = false)
        value: Optional<String>,
        @Parameter(description = "Filter by context id. Beware: handled separately from other parameters!")
        @RequestParam(value = "contextId", required = false)
        contextId: Optional<String>,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false)
        isVisible: Optional<Boolean>,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "false")
        filterProgrammes: Boolean,
        @Parameter(description = "Id to root id in context.")
        @RequestParam(value = "rootId", required = false)
        rootId: Optional<URI>,
        @Parameter(description = "Id to parent id in context.")
        @RequestParam(value = "parentId", required = false)
        parentId: Optional<URI>,
    ): List<NodeDTO> {
        val metadataFilters = MetadataFilters(key, value, isVisible)
        val isRootOrContext = if (isRoot.isPresent) isRoot else isContext
        val defaultNodeTypes = getDefaultNodeTypes(nodeType, contentUri, contextId, isRootOrContext, metadataFilters)
        return nodeService.getNodesByType(
            Optional.of(defaultNodeTypes),
            language,
            publicIds,
            contentUri,
            contextId,
            isRoot,
            isContext,
            metadataFilters,
            includeContexts,
            filterProgrammes,
            true,
            rootId,
            parentId,
        )
    }

    @GetMapping("/search")
    @Operation(summary = "Search all nodes")
    @Transactional(readOnly = true)
    fun searchNodes(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "How many results to return per page")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
        @Parameter(description = "Which page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Query to search names")
        @RequestParam(value = "query", required = false)
        query: Optional<String>,
        @Parameter(description = "Ids to fetch for query")
        @RequestParam(value = "ids", required = false)
        ids: Optional<List<String>>,
        @Parameter(description = "ContentURIs to fetch for query")
        @RequestParam(value = "contentUris", required = false)
        contentUris: Optional<List<String>>,
        @Parameter(description = "Filter by nodeType")
        @RequestParam(value = "nodeType", required = false)
        nodeType: Optional<List<NodeType>>,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "false")
        filterProgrammes: Boolean,
        @Parameter(description = "Id to root id in context to select. Does not affect search results")
        @RequestParam(value = "rootId", required = false)
        rootId: Optional<URI>,
        @Parameter(description = "Id to parent id in context to select. Does not affect search results")
        @RequestParam(value = "parentId", required = false)
        parentId: Optional<URI>,
    ): SearchResultDTO<NodeDTO> = searchService.searchByNodeType(
        query,
        ids,
        contentUris,
        language,
        includeContexts,
        filterProgrammes,
        pageSize,
        page,
        nodeType,
        Optional.empty(),
        rootId,
        parentId,
    )

    @PostMapping("/search")
    @Operation(summary = "Search all nodes")
    @Transactional(readOnly = true)
    fun searchNodes(@RequestBody searchBodyParams: NodeSearchBody): SearchResultDTO<NodeDTO> = searchService.searchByNodeType(
        searchBodyParams.query,
        searchBodyParams.ids,
        searchBodyParams.contentUris,
        searchBodyParams.language,
        searchBodyParams.includeContexts,
        searchBodyParams.filterProgrammes,
        searchBodyParams.pageSize,
        searchBodyParams.page,
        searchBodyParams.nodeType,
        searchBodyParams.customFields,
        searchBodyParams.rootId,
        searchBodyParams.parentId,
    )

    @GetMapping("/page")
    @Operation(summary = "Gets all nodes paginated")
    @Transactional(readOnly = true)
    fun getNodePage(
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
        language: String,
        @Parameter(description = "The page to fetch")
        @RequestParam(value = "page", defaultValue = "1")
        page: Int,
        @Parameter(description = "Size of page to fetch")
        @RequestParam(value = "pageSize", defaultValue = "10")
        pageSize: Int,
        @Parameter(name = "nodeType", description = "Filter by nodeType")
        nodeType: Optional<NodeType>,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "false")
        filterProgrammes: Boolean,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false, defaultValue = "true")
        isVisible: Boolean,
    ): SearchResultDTO<NodeDTO> {
        require(page >= 1) { "page parameter must be bigger than 0" }

        val ids = nodeType
            .map { nt -> nodeRepository.findIdsByTypePaginated(PageRequest.of(page - 1, pageSize), nt) }
            .orElseGet { nodeRepository.findIdsPaginated(PageRequest.of(page - 1, pageSize)) }
        val results = nodeRepository.findByIds(ids.content)
        val contents = results.map { node ->
            NodeDTO(
                Optional.empty(),
                Optional.empty(),
                node,
                NodeConnectionType.BRANCH,
                language,
                Optional.empty(),
                includeContexts,
                filterProgrammes,
                isVisible,
                false,
            )
        }
        return SearchResultDTO(ids.totalElements, page, pageSize, contents)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single node")
    @Transactional(readOnly = true)
    fun getNode(
        @PathVariable("id") id: URI,
        @Parameter(description = "Id to root id in context.")
        @RequestParam(value = "rootId", required = false)
        rootId: Optional<URI>,
        @Parameter(description = "Id to parent id in context.")
        @RequestParam(value = "parentId", required = false)
        parentId: Optional<URI>,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "true")
        filterProgrammes: Boolean,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false, defaultValue = "true")
        isVisible: Boolean,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
    ): NodeDTO = nodeService.getNode(id, language, rootId, parentId, includeContexts, filterProgrammes, isVisible)

    @PostMapping
    @Operation(
        summary = "Creates a new node",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun createNode(
        @Parameter(name = "connection", description = "The new node") @RequestBody command: NodePostPut,
    ): ResponseEntity<Unit> {
        try {
            val entity = Node(command.nodeType)
            val locked = qualityEvaluationService.lockQualityEvaluationIfNeeded(command)
            validateAndAssignId(entity, command)
            val oldGrade = entity.qualityEvaluationGrade
            command.apply(entity)
            nodeRepository.saveAndFlush(entity)
            contextUpdaterService.updateContexts(entity)
            if (locked) {
                qualityEvaluationService.updateQualityEvaluationOfParentsFromFreshlyLoadedNode(entity, oldGrade, command)
            }
            return ResponseEntity.created(URI.create("$location/${entity.publicId}")).build()
        } catch (e: DataIntegrityViolationException) {
            handleDuplicateId(command)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a single node",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun updateNode(
        @PathVariable("id") id: URI,
        @Parameter(name = "node", description = "The updated node. Fields not included will be set to null.")
        @RequestBody
        command: NodePostPut,
    ) {
        val locked = qualityEvaluationService.lockQualityEvaluationIfNeeded(command)
        val entity = nodeRepository.getByPublicId(id)
        validateUrn(id, entity)
        val oldGrade = entity.qualityEvaluationGrade
        command.apply(entity)
        contextUpdaterService.updateContexts(entity)
        if (locked) {
            qualityEvaluationService.updateQualityEvaluationOfParentsFromFreshlyLoadedNode(entity, oldGrade, command)
        }
    }

    @PutMapping("/{id}/publish")
    @Operation(
        summary = "Publishes a node hierarchy to a version",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    @Deprecated("Endpoint deprecated", level = DeprecationLevel.WARNING)
    fun publishNode(
        @PathVariable("id") id: URI,
        @Parameter(
            description = "Version id to publish from. Can be omitted to publish from default.",
            example = "urn:version:1",
        )
        @RequestParam(value = "sourceId", required = false)
        sourceId: Optional<URI>,
        @Parameter(description = "Version id to publish to.", example = "urn:version:2")
        @RequestParam(value = "targetId")
        targetId: URI,
    ): Unit = throw UnsupportedOperationException("This endpoint is deprecated")

    @GetMapping("/{id}/nodes")
    @Operation(summary = "Gets all children for this node")
    @Transactional(readOnly = true)
    fun getChildren(
        @Parameter(name = "id", required = true) @PathVariable("id") id: URI,
        @Parameter(
            description = "Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^)",
        )
        @RequestParam(value = "nodeType", required = false)
        nodeType: Optional<List<NodeType>>,
        @Parameter(description = "Only connections of given type are returned")
        @RequestParam(value = "connectionTypes", defaultValue = "BRANCH")
        connectionTypes: List<NodeConnectionType>,
        @Parameter(description = "If true, children are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "false")
        filterProgrammes: Boolean,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false, defaultValue = "true")
        isVisible: Boolean,
    ): List<NodeChildDTO> {
        val node = nodeRepository.findFirstByPublicId(id).orElseThrow { NotFoundException("Node", id) }

        val nodeTypes = getDefaultNodeTypes(
            nodeType,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            MetadataFilters(Optional.empty(), Optional.empty(), Optional.empty()),
        )
        val childrenIds: List<URI> = if (recursive) {
            recursiveNodeTreeService.getRecursiveNodes(node, nodeTypes)
                .map { it.id }
        } else {
            node.childConnections
                .filter { connectionTypes.contains(it.connectionType) }
                .mapNotNull { it.child.orElse(null) }
                .filter { nodeTypes.contains(it.nodeType) }
                .map { it.publicId }
        }
        val children = nodeConnectionRepository.findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(childrenIds)

        val returnList = children.map { nodeConnection ->
            NodeChildDTO(
                Optional.of(node),
                nodeConnection,
                language,
                includeContexts,
                filterProgrammes,
                isVisible,
            )
        }

        val filtered = returnList.filter {
            childrenIds.contains(it.parentId) || node.publicId == it.parentId
        }

        return treeSorter.sortList(filtered).distinct()
    }

    @GetMapping("/{id}/connections")
    @Operation(summary = "Gets all parents and children this node is connected to")
    @Transactional(readOnly = true)
    fun getAllConnections(@PathVariable("id") id: URI): List<ConnectionDTO> = nodeService.getAllConnections(id)

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletes a single node by id",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        nodeService.delete(id)
    }

    @GetMapping("/{id}/resources")
    @Operation(
        summary = "Gets all resources for the given node",
        tags = ["nodes"],
    )
    @Transactional(readOnly = true)
    fun getResources(
        @Parameter(name = "id", required = true) @PathVariable("id") nodeId: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false, defaultValue = "true")
        includeContexts: Boolean,
        @Parameter(description = "Filter out programme contexts")
        @RequestParam(value = "filterProgrammes", required = false, defaultValue = "false")
        filterProgrammes: Boolean,
        @Parameter(description = "Filter contexts by visibility")
        @RequestParam(value = "isVisible", required = false, defaultValue = "true")
        isVisible: Boolean,
        @Parameter(description = "If true, resources from children are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
        @Parameter(
            description = "Select by resource type id(s). If not specified, resources of all types will be returned. Multiple ids may be separated with comma or the parameter may be repeated for each id.",
        )
        @RequestParam(value = "type", required = false)
        resourceTypeIds: Optional<List<URI>>,
        @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
        @RequestParam(value = "relevance", required = false)
        relevance: Optional<URI>,
    ): List<NodeChildDTO> = nodeService.getResourcesByNodeId(
        nodeId,
        resourceTypeIds,
        relevance,
        Optional.of(language),
        recursive,
        includeContexts,
        filterProgrammes,
        isVisible,
    )

    @GetMapping("{id}/full")
    @Operation(
        summary = "Gets node including information about all parents, and resourceTypes for this resource. Can be replaced with regular get-endpoint and traversing contexts",
        deprecated = true,
    )
    @Transactional(readOnly = true)
    @Deprecated("Use /{id} and traverse contexts", level = DeprecationLevel.WARNING)
    fun getNodeFull(
        @PathVariable("id") id: URI,
        @Parameter(description = "ISO-639-1 language code", example = "nb")
        @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
        language: String,
        @Parameter(description = "Include all contexts")
        @RequestParam(value = "includeContexts", required = false)
        includeContexts: Boolean,
    ): NodeWithParents {
        val node = nodeService.getNode(id)
        return NodeWithParents(node, language, includeContexts)
    }

    @PutMapping("/{id}/makeResourcesPrimary")
    @Operation(
        summary = "Makes all connected resources primary",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    fun makeResourcesPrimary(
        @Parameter(name = "id", required = true) @PathVariable("id") nodeId: URI,
        @Parameter(description = "If true, children are fetched recursively")
        @RequestParam(value = "recursive", required = false, defaultValue = "false")
        recursive: Boolean,
    ): ResponseEntity<Boolean> = ResponseEntity.of(Optional.of(nodeService.makeAllResourcesPrimary(nodeId, recursive)))

    @PostMapping("{id}/clone")
    @Operation(
        summary = "Clones a node, presumably a resource, including resource-types and translations",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun cloneResource(
        @Parameter(name = "id", description = "Id of node to clone", example = "urn:resource:1")
        @PathVariable("id")
        publicId: URI,
        @Parameter(name = "node", description = "Object containing contentUri. Other values are ignored.")
        @RequestBody
        command: NodePostPut,
    ): ResponseEntity<Unit> {
        val entity = nodeService.cloneNode(publicId, command.contentUri)
        val locationUri = URI.create("$location/${entity.publicId}")
        return ResponseEntity.created(locationUri).build()
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    @Transactional(readOnly = true)
    fun getMetadata(@PathVariable("id") id: URI): MetadataDTO {
        val node = nodeRepository.findByPublicId(id) ?: throw NotFoundException("Node", id)
        return MetadataDTO(node.metadata)
    }

    @PutMapping("/{id}/metadata")
    @Operation(
        summary = "Updates metadata for entity",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    fun putMetadata(
        @PathVariable("id") id: URI,
        @RequestBody entityToUpdate: MetadataPUT,
    ): MetadataDTO {
        val node = nodeRepository.findByPublicId(id) ?: throw NotFoundException("Node", id)
        val result = node.metadata.mergeWith(entityToUpdate)
        contextUpdaterService.updateContexts(node)
        return MetadataDTO(result)
    }


}
