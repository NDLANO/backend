/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

public class JsonTranslation implements Serializable, Translation {
    @JsonProperty("name")
    String name;

    @JsonProperty("languageCode")
    String languageCode;

    @JsonIgnore
    Translatable parent;

    public JsonTranslation(JsonTranslation js) {
        this.name = js.getName();
        this.languageCode = js.getLanguageCode();
    }

    public JsonTranslation(String languageCode) {
        this.languageCode = languageCode;
    }

    public JsonTranslation(String name, String languageCode) {
        this.name = name;
        this.languageCode = languageCode;
    }

    public JsonTranslation() {}

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getLanguageCode() {
        return this.languageCode;
    }

    @JsonIgnore
    public Translatable getParent() {
        return this.parent;
    }

    @JsonIgnore
    public void setParent(Translatable parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (JsonTranslation) o;
        return Objects.equals(this.getLanguageCode(), that.getLanguageCode())
                && Objects.equals(this.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName(), this.getLanguageCode());
    }
}
