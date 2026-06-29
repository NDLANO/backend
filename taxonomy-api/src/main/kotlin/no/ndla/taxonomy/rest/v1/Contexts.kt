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
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.dtos.ContextDTO
import no.ndla.taxonomy.rest.v1.dtos.ContextPOST
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.ContextUpdaterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/contexts", "/v1/contexts/"])
@Transactional(readOnly = true)
class Contexts(
    val nodeRepository: NodeRepository,
    val contextUpdaterService: ContextUpdaterService,
) {

  @GetMapping
  @Operation(summary = "Gets id of all nodes registered as context")
  fun getAllContexts(
      @Parameter(description = "ISO-639-1 language code", example = "nb")
      @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
      language: String
  ): List<ContextDTO> =
      nodeRepository
          .findAllByContextIncludingCachedUrlsAndTranslations(true)
          .map {
            ContextDTO(it.publicId, it.primaryPath.getOrNull(), it.getTranslatedName(language))
          }
          .sortedBy { it.id }

  @PostMapping
  @Operation(
      summary = "Registers a new node as context",
      description =
          "All subjects are already contexts and may not be added again. The node to register as context must exist already.",
      security = [SecurityRequirement(name = "oauth")],
  )
  @Created201ApiResponse
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun createContext(
      @Parameter(
          name = "context",
          description = "object containing public id of the node to be registered as context",
      )
      @RequestBody
      command: ContextPOST
  ): ResponseEntity<Unit> {
    val node = nodeRepository.getByPublicId(command.id)
    node.isContext = true
    val location = URI.create("/v1/contexts/${node.publicId}")

    contextUpdaterService.updateContexts(node)

    return ResponseEntity.created(location).build()
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Removes context registration from node",
      description = "Does not remove the underlying node, only marks it as not being a context",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun deleteContext(@PathVariable("id") id: URI) {
    val node = nodeRepository.getByPublicId(id)
    node.isContext = false
    contextUpdaterService.updateContexts(node)
  }
}
