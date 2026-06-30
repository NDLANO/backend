/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.UpdatableDto;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import no.ndla.taxonomy.service.dtos.TechnicalEvaluationDTO;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;

@Schema(name = "NodePostPut")
public class NodePostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the node_id to this value. If omitted, an uuid will be assigned automatically.")
    public Optional<String> nodeId = Optional.empty();

    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Schema(description = "Type of node.", example = "topic")
    public NodeType nodeType;

    @JsonProperty
    @Schema(
            description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.",
            example = "urn:article:1")
    public Optional<URI> contentUri = Optional.empty();

    @JsonProperty
    @Schema(description = "The name of the node. Required on create.", example = "Trigonometry")
    public Optional<String> name = Optional.empty();

    @JsonProperty
    @Deprecated
    @Schema(description = "The node is a root node. Default is false. Only used if present.")
    public Optional<Boolean> root = Optional.empty();

    @JsonProperty
    @Schema(description = "The node is the root in a context. Default is false. Only used if present.")
    public Optional<Boolean> context = Optional.empty();

    @Schema(description = "The node is visible. Default is true.")
    public Optional<Boolean> visible = Optional.empty();

    @Schema(description = "ResourceType public ids to assign to the node. Only works for nodes of type RESOURCE")
    public Optional<List<URI>> resourceTypes = Optional.empty();

    @JsonProperty
    @Schema(description = "The language used at create time. Used to set default translation.", example = "nb")
    public String language = Constants.DefaultLanguage;

    @JsonProperty
    @Schema(
            implementation = QualityEvaluationDTO.class,
            description =
                    "The quality evaluation of the node. Consist of a score from 1 to 5 and a comment. Can be null to remove existing evaluation.",
            types = {"object", "null"})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public UpdateOrDelete<QualityEvaluationDTO> qualityEvaluation = UpdateOrDelete.Default.INSTANCE;

    @JsonProperty
    @Schema(
            implementation = TechnicalEvaluationDTO.class,
            description =
                    "The technical evaluation of the node. Contains a flag and an optional comment. Can be null to remove existing evaluation.",
            types = {"object", "null"})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public UpdateOrDelete<TechnicalEvaluationDTO> technicalEvaluation = UpdateOrDelete.Default.INSTANCE;

    @JsonProperty
    @Schema(description = "The translations for the node. Contains an array of translations in different languages")
    public Optional<List<TranslationDTO>> translations = Optional.empty();

    public Optional<String> getNodeId() {
        return nodeId;
    }

    @JsonIgnore
    public URI getPublicId() {
        return URI.create("urn:" + nodeType.getName() + ":" + nodeId.get());
    }

    @Override
    public void apply(Node node) {
        if (node.getIdent() == null) {
            node.setIdent(UUID.randomUUID().toString());
        }
        if (getNodeId().isPresent()) {
            node.setPublicId(getPublicId());
        }
        if (nodeType != null) {
            node.setNodeType(nodeType);
        }

        switch (this.qualityEvaluation) {
            case UpdateOrDelete.Delete _ -> {
                node.setQualityEvaluation(null);
                node.setQualityEvaluationComment(Optional.empty());
            }
            case UpdateOrDelete.Update<?> u -> {
                var qe = (QualityEvaluationDTO) u.getValue();
                node.setQualityEvaluation(qe.getGrade());
                node.setQualityEvaluationComment(Optional.ofNullable(qe.getNote()));
            }
            default -> {}
        }

        switch (this.technicalEvaluation) {
            case UpdateOrDelete.Delete _ -> {
                node.setRequiresTechnicalEvaluation(Optional.empty());
                node.setTechnicalEvaluationComment(Optional.empty());
            }
            case UpdateOrDelete.Update<?> u -> {
                var te = (TechnicalEvaluationDTO) u.getValue();
                node.setRequiresTechnicalEvaluation(Optional.of(te.getRequiresEvaluation()));
                node.setTechnicalEvaluationComment(
                        te.getRequiresEvaluation() ? Optional.ofNullable(te.getComment()) : Optional.empty());
            }
            default -> {}
        }

        translations.ifPresent(ts -> {
            node.setTranslations(ts.stream()
                    .map(t -> new JsonTranslation(t.name, t.language))
                    .toList());
        });

        if (node.getNodeType() == NodeType.RESOURCE) {
            resourceTypes.ifPresent(rts -> {
                node.clearResourceTypes();
                rts.forEach(rt -> {
                    var resourceType = ResourceType.Companion.findByPublicId(rt);
                    if (resourceType == null) throw new InvalidArgumentServiceException("Unknown resource type:" + rt);
                    node.addResourceType(resourceType);
                });
            });
        }

        root.ifPresent(node::setContext);
        context.ifPresent(node::setContext);
        name.ifPresent(node::setName);
        contentUri.ifPresent(node::setContentUri);
        visible.ifPresent(node::setVisible);
        // Add translation only on post
        name.ifPresent(name -> {
            if (node.getId() == null && translations.isEmpty()) {
                node.addTranslation(name, language);
            }
        });
    }
}
