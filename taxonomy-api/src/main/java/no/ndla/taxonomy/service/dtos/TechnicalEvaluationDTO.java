/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;

@Schema(requiredProperties = {"requiresEvaluation"})
public class TechnicalEvaluationDTO {
    @JsonProperty
    @Schema(description = "Whether this node requires a technical evaluation.")
    private boolean requiresEvaluation;

    @JsonProperty
    @Schema(description = "Notes for the technical evaluation of this node.")
    private Optional<String> comment = Optional.empty();

    public TechnicalEvaluationDTO(boolean requiresEvaluation, Optional<String> comment) {
        this.requiresEvaluation = requiresEvaluation;
        this.comment = comment;
    }

    public static Optional<TechnicalEvaluationDTO> fromNode(Node node) {
        return node.requiresTechnicalEvaluation()
                .map(requiresEvaluation ->
                        new TechnicalEvaluationDTO(requiresEvaluation, node.getTechnicalEvaluationComment()));
    }

    public boolean requiresEvaluation() {
        return requiresEvaluation;
    }

    public void setRequiresEvaluation(boolean requiresEvaluation) {
        this.requiresEvaluation = requiresEvaluation;
    }

    public Optional<String> getComment() {
        return comment;
    }

    public void setComment(Optional<String> comment) {
        this.comment = comment;
    }
}
