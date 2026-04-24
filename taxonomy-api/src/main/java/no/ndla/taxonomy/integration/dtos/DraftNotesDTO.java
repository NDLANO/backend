/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record DraftNotesDTO(
        @JsonProperty long draftId, @JsonProperty Collection<String> notes) implements Serializable {

    public static DraftNotesDTO fromNote(long draftId, String note) {
        return new DraftNotesDTO(draftId, List.of(note));
    }
}
