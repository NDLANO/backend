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
import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import no.ndla.taxonomy.rest.NotFoundHttpResponseException
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse
import no.ndla.taxonomy.service.UpdatableDto
import no.ndla.taxonomy.service.VersionService
import no.ndla.taxonomy.service.dtos.VersionDTO
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException
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
@RequestMapping(path = ["/v1/versions", "/v1/versions/"])
class Versions(
    private val versionService: VersionService,
    private val versionRepository: VersionRepository,
) {

    private val location: String by lazy { controllerLocation(javaClass) }

    @GetMapping
    @Operation(summary = "Gets all versions")
    @Transactional(readOnly = true)
    fun getAllVersions(
        @Parameter(description = "Version type", example = "PUBLISHED")
        @RequestParam(value = "type", required = false, defaultValue = "")
        versionType: Optional<VersionType>,
        @Parameter(description = "Version hash", example = "ndla")
        @RequestParam(value = "hash", required = false)
        hash: Optional<String>,
    ): List<VersionDTO> {
        if (versionType.isPresent) return versionService.getVersionsOfType(versionType.get())
        return hash
            .map { s ->
                listOf(
                    versionRepository
                        .findFirstByHash(s)
                        .map { VersionDTO(it) }
                        .orElseThrow { NotFoundHttpResponseException("Version not found") },
                )
            }
            .orElseGet { versionService.getVersions() }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single version")
    @Transactional(readOnly = true)
    fun getVersion(@PathVariable("id") id: URI): VersionDTO = versionRepository
        .findFirstByPublicId(id)
        .map { VersionDTO(it) }
        .orElseThrow { NotFoundHttpResponseException("Version not found") }

    @PostMapping
    @Operation(
        summary = "Creates a new version",
        security = [SecurityRequirement(name = "oauth")],
    )
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    fun createVersion(
        @Parameter(description = "Base new version on version with this id")
        @RequestParam(value = "sourceId")
        sourceId: Optional<URI>,
        @Parameter(name = "version", description = "The new version") @RequestBody command: VersionPostPut,
    ): ResponseEntity<Unit> {
        val version = versionService.createNewVersion(sourceId, command)
        val locationUri = URI.create("$location/${version.publicId}")
        return ResponseEntity.created(locationUri).build()
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Updates a version",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    fun updateVersion(
        @PathVariable("id") id: URI,
        @Parameter(name = "version", description = "The updated version.") @RequestBody command: VersionPostPut,
    ) {
        updateEntity(id, command)
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletes a version by publicId",
        security = [SecurityRequirement(name = "oauth")],
    )
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteEntity(@PathVariable("id") id: URI) {
        val version = versionRepository.findFirstByPublicId(id)
        if (version.isEmpty || version.get().isLocked) {
            throw InvalidArgumentServiceException("Cannot delete locked version")
        }
        versionService.delete(id)
    }

    @PutMapping("/{id}/publish")
    @Operation(
        summary = "Publishes a version",
        security = [SecurityRequirement(name = "oauth")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    fun publishVersion(@PathVariable("id") id: URI) {
        val version = versionRepository.findFirstByPublicId(id)
        if (version.isEmpty || version.get().versionType != VersionType.BETA) {
            throw InvalidArgumentServiceException("Version has wrong type")
        }
        versionService.publishBetaAndArchiveCurrent(id)
    }

    private fun updateEntity(id: URI, command: UpdatableDto<Version>): Version {
        val entity = versionRepository.getByPublicId(id)
        validateUrn(id, entity)
        command.apply(entity)
        return entity
    }
}
