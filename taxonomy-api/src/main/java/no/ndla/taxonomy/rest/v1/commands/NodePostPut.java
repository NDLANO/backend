/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.UpdatableDto;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import no.ndla.taxonomy.service.dtos.TechnicalEvaluationDTO;

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

    @JsonProperty
    @Schema(description = "The language used at create time. Used to set default translation.", example = "nb")
    public String language = Constants.DefaultLanguage;

    @JsonProperty
    @Schema(
            implementation = QualityEvaluationDTO.class,
            description =
                    "The quality evaluation of the node. Consist of a score from 1 to 5 and a comment. Can be null to remove existing evaluation.",
            types = {"object", "null"})
    @JsonDeserialize(using = QualityEvaluationDTODeserializer.class)
    @JsonSerialize(using = QualityEvaluationDTOSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public UpdateOrDelete<QualityEvaluationDTO> qualityEvaluation = UpdateOrDelete.Default();

    @JsonProperty
    @Schema(
            implementation = TechnicalEvaluationDTO.class,
            description =
                    "The technical evaluation of the node. Contains a flag and an optional comment. Can be null to remove existing evaluation.",
            types = {"object", "null"})
    @JsonDeserialize(using = TechnicalEvaluationDTODeserializer.class)
    @JsonSerialize(using = TechnicalEvaluationDTOSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public UpdateOrDelete<TechnicalEvaluationDTO> technicalEvaluation = UpdateOrDelete.Default();

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

        if (this.qualityEvaluation.isDelete()) {
            node.setQualityEvaluation(null);
            node.setQualityEvaluationComment(Optional.empty());
        } else {
            this.qualityEvaluation.getValue().ifPresent(qe -> {
                node.setQualityEvaluation(qe.getGrade());
                node.setQualityEvaluationComment(qe.getNote());
            });
        }

        if (this.technicalEvaluation.isDelete()) {
            node.setRequiresTechnicalEvaluation(Optional.empty());
            node.setTechnicalEvaluationComment(Optional.empty());
        } else {
            this.technicalEvaluation.getValue().ifPresent(te -> {
                node.setRequiresTechnicalEvaluation(Optional.of(te.requiresEvaluation()));
                node.setTechnicalEvaluationComment(te.requiresEvaluation() ? te.getComment() : Optional.empty());
            });
        }

        root.ifPresent(node::setContext);
        context.ifPresent(node::setContext);
        name.ifPresent(node::setName);
        contentUri.ifPresent(node::setContentUri);
        visible.ifPresent(node::setVisible);
        // Add translation only on post
        name.ifPresent(name -> {
            if (node.getId() == null) {
                node.addTranslation(name, language);
            }
        });
    }
}
