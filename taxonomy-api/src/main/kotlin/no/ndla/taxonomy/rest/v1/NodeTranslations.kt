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
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT
import no.ndla.taxonomy.service.dtos.TranslationDTO
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/nodes/{id}/translations", "/v1/nodes/{id}/translations/"])
@Deprecated("use /v1/nodes")
class NodeTranslations(val nodeRepository: NodeRepository) {

  @GetMapping
  @Operation(summary = "Gets all translations for a single node")
  @Transactional(readOnly = true)
  fun getAllNodeTranslations(@PathVariable("id") id: URI): List<TranslationDTO> {
    return nodeRepository.getByPublicId(id).translations.map { TranslationDTO(it) }
  }

  @GetMapping("/{language}")
  @Operation(summary = "Gets a single translation for a single node")
  @Transactional(readOnly = true)
  fun getNodeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
  ): TranslationDTO {
    return nodeRepository.getByPublicId(id).getTranslation(language).getOrNull()?.let {
      TranslationDTO(it)
    } ?: throw NotFoundException("translation with language code $language for node $id")
  }

  @PutMapping("/{language}")
  @Operation(
      summary = "Creates or updates a translation of a node",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun createUpdateNodeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
      @Parameter(name = "command", description = "The new or updated translation")
      @RequestBody
      command: TranslationPUT,
  ) {
    val node = nodeRepository.getByPublicId(id)
    node.addTranslation(command.name, language)
    nodeRepository.save(node)
  }

  @DeleteMapping("/{language}")
  @Operation(
      summary = "Deletes a translation",
      security = [SecurityRequirement(name = "oauth")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
  @Transactional
  fun deleteNodeTranslation(
      @PathVariable("id") id: URI,
      @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
      @PathVariable("language")
      language: String,
  ) {
    val node = nodeRepository.getByPublicId(id)
    node.getTranslation(language).ifPresent {
      node.removeTranslation(language)
      nodeRepository.save(node)
    }
  }
}
