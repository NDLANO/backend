/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import no.ndla.taxonomy.service.dtos.TechnicalEvaluationDTO;

public class TechnicalEvaluationDTOSerializer extends UpdateOrDelete.Serializer<TechnicalEvaluationDTO> {
    @Override
    protected void serializeInner(
            TechnicalEvaluationDTO value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeObject(value);
    }
}
