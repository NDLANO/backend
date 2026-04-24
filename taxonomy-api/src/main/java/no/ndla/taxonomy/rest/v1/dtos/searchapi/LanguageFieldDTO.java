/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import no.ndla.taxonomy.domain.LanguageField;
import org.springframework.data.util.Pair;

@Schema(name = "LanguageField", type = "object")
public class LanguageFieldDTO<V> extends HashMap<String, V> {

    public LanguageFieldDTO() {}

    public LanguageFieldDTO(List<Pair<String, V>> translations) {
        translations.forEach(t -> this.put(t.getFirst(), t.getSecond()));
    }

    public static LanguageFieldDTO<String> fromLanguageField(LanguageField<String> languageField) {
        var dto = new LanguageFieldDTO<String>();
        Set<String> keySet = languageField.keySet();
        keySet.forEach(key -> {
            dto.put(key, languageField.get(key));
        });
        return dto;
    }

    public static LanguageFieldDTO<List<String>> fromLanguageFieldList(LanguageField<List<String>> languageField) {
        var dto = new LanguageFieldDTO<List<String>>();
        Set<String> keySet = languageField.keySet();
        keySet.forEach(key -> {
            dto.put(key, languageField.get(key));
        });
        return dto;
    }

    // Hide inherited method from documentation
    @Override
    @Schema(hidden = true)
    public boolean isEmpty() {
        return super.isEmpty();
    }
}
