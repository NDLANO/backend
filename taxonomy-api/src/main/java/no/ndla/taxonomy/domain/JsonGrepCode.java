/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;

public record JsonGrepCode(
        @JsonProperty("code") String code,
        @JsonProperty("created_at") String created_at,
        @JsonProperty("updated_at") String updated_at)
        implements Serializable {
    public JsonGrepCode(JsonGrepCode copy) {
        this(copy.code, copy.created_at, copy.updated_at);
    }

    public JsonGrepCode(String code) {
        this(code, Instant.now().toString(), Instant.now().toString());
    }

    public String getCode() {
        return this.code;
    }
}
