/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import no.ndla.taxonomy.domain.Grade;
import no.ndla.taxonomy.domain.Node;

@Schema(requiredProperties = {"grade"})
public class QualityEvaluationDTO {
    @JsonProperty
    @Schema(description = "The grade (1-5) of the article")
    private Grade grade;

    @JsonProperty
    @Schema(description = "Note explaining the score")
    private Optional<String> note;

    public QualityEvaluationDTO(Grade grade, Optional<String> note) {
        this.grade = grade;
        this.note = note;
    }

    public static Optional<QualityEvaluationDTO> fromNode(Node node) {
        return node.getQualityEvaluationGrade()
                .map(grade -> new QualityEvaluationDTO(grade, node.getQualityEvaluationNote()));
    }

    public Optional<String> getNote() {
        return note;
    }

    public void setNote(Optional<String> note) {
        this.note = note;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }
}
