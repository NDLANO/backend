/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface Translatable {

    List<JsonTranslation> getTranslations();

    void setTranslations(List<JsonTranslation> translations);

    default void clearTranslations() {
        setTranslations(new ArrayList<>());
    }

    default Optional<JsonTranslation> getTranslation(String languageCode) {
        return getTranslations().stream()
                .filter(translation -> translation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    default JsonTranslation addTranslation(JsonTranslation nodeTranslation) {
        if (nodeTranslation.getParent() != this) {
            nodeTranslation.setParent(this);
        }
        var newTranslations = new ArrayList<>(getTranslations().stream()
                .filter(t -> !Objects.equals(t.getLanguageCode(), nodeTranslation.getLanguageCode()))
                .toList());
        newTranslations.add(nodeTranslation);
        setTranslations(newTranslations);
        return nodeTranslation;
    }

    default JsonTranslation addTranslation(String name, String languageCode) {
        var nodeTranslation = new JsonTranslation(name, languageCode);
        return this.addTranslation(nodeTranslation);
    }

    default void removeTranslation(JsonTranslation translation) {
        translation.setParent(null);
        var newTranslations = new ArrayList<>(
                getTranslations().stream().filter(t -> t != translation).toList());
        setTranslations(newTranslations);
    }

    default void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }
}
