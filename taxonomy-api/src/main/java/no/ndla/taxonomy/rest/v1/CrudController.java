/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Grade;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class CrudController<T extends DomainEntity> {
    protected TaxonomyRepository<T> repository;
    protected ContextUpdaterService contextUpdaterService;
    protected NodeService nodeService;
    protected QualityEvaluationService qualityEvaluationService;
    protected ResourceTypeService resourceTypeService;

    private static final Map<Class<?>, String> locations = new HashMap<>();
    private final URNValidator validator = new URNValidator();

    protected CrudController(
            TaxonomyRepository<T> repository,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService,
            QualityEvaluationService qualityEvaluationService,
            ResourceTypeService resourceTypeService) {
        this.repository = repository;
        this.contextUpdaterService = contextUpdaterService;
        this.nodeService = nodeService;
        this.qualityEvaluationService = qualityEvaluationService;
        this.resourceTypeService = resourceTypeService;
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    protected abstract void deleteEntity(@PathVariable("id") URI id);

    protected Optional<Grade> getOldGrade(T entity) {
        if (entity instanceof Node node) {
            return node.getQualityEvaluationGrade();
        }
        return Optional.empty();
    }

    @Operation(
            summary = "Updates a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    protected T updateEntity(URI id, UpdatableDto<T> command) {
        var qualityEvaluationUpdateLocked =
                qualityEvaluationService != null && qualityEvaluationService.lockQualityEvaluationIfNeeded(command);

        T entity = repository.getByPublicId(id);
        validator.validate(id, entity);

        var oldGrade = getOldGrade(entity);

        command.apply(entity);

        if (entity instanceof Node node) {
            if (contextUpdaterService != null) contextUpdaterService.updateContexts(node);
            if (qualityEvaluationUpdateLocked) {
                qualityEvaluationService.updateQualityEvaluationOfParentsFromFreshlyLoadedNode(node, oldGrade, command);
            }
        }

        if (entity instanceof ResourceType resourceType && resourceTypeService != null) {
            resourceTypeService.shiftOrderAfterInsertUpdate(resourceType);
        }

        return entity;
    }

    @Operation(
            summary = "Creates a single entity",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    protected ResponseEntity<Void> createEntity(T entity, UpdatableDto<T> command) {
        try {
            var qualityEvaluationUpdateLocked =
                    qualityEvaluationService != null && qualityEvaluationService.lockQualityEvaluationIfNeeded(command);

            command.getId().ifPresent(id -> {
                validator.validate(id, entity);
                entity.setPublicId(id);
            });
            var oldGrade = getOldGrade(entity);

            command.apply(entity);
            URI location = URI.create(getLocation() + "/" + entity.getPublicId());
            repository.saveAndFlush(entity);

            if (entity instanceof Node node) {
                if (contextUpdaterService != null) contextUpdaterService.updateContexts(node);
                if (qualityEvaluationUpdateLocked) {
                    qualityEvaluationService.updateQualityEvaluationOfParentsFromFreshlyLoadedNode(
                            node, oldGrade, command);
                }
            }

            if (entity instanceof ResourceType resourceType && resourceTypeService != null) {
                resourceTypeService.shiftOrderAfterInsertUpdate(resourceType);
            }

            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            command.getId().ifPresent(id -> {
                throw new DuplicateIdException(id.toString());
            });

            throw new DuplicateIdException();
        }
    }

    protected String getLocation() {
        return locations.computeIfAbsent(
                getClass(), aClass -> aClass.getAnnotation(RequestMapping.class).path()[0]);
    }
}
