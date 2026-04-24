/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;

public class QualityEvaluationDTOSerializer extends UpdateOrDelete.Serializer<QualityEvaluationDTO> {
    @Override
    protected void serializeInner(
            QualityEvaluationDTO value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeObject(value);
    }
}
