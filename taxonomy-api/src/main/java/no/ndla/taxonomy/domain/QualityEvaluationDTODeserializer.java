/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;

public class QualityEvaluationDTODeserializer extends UpdateOrDelete.Deserializer<QualityEvaluationDTO> {
    private Optional<String> getNote(JsonNode node) {
        var hasNote = node.has("note");
        var noteNode = node.get("note");
        if (hasNote && noteNode.isTextual()) {
            return Optional.of(node.get("note").asText());
        }
        return Optional.empty();
    }

    @Override
    protected QualityEvaluationDTO deserializeInner(JsonNode node) {
        var gradeInt = node.get("grade").asInt();
        var grade = Grade.fromInt(gradeInt);
        var note = getNote(node);
        return new QualityEvaluationDTO(grade, note);
    }
}
