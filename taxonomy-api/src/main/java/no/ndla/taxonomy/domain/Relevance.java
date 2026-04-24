/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;

public enum Relevance {
    CORE(
            "urn:relevance:core",
            Set.of(
                    new JsonTranslation("Kjernestoff", "nb"),
                    new JsonTranslation("Kjernestoff", "nn"),
                    new JsonTranslation("Core content", "en"),
                    new JsonTranslation("Guovddášávnnas", "se"))),
    SUPPLEMENTARY(
            "urn:relevance:supplementary",
            Set.of(
                    new JsonTranslation("Tilleggsstoff", "nb"),
                    new JsonTranslation("Tilleggstoff", "nn"),
                    new JsonTranslation("Supplementary content", "en"),
                    new JsonTranslation("Lassiávnnas", "se")));

    final URI id;
    final Set<Translation> translations;

    public URI getPublicId() {
        return id;
    }

    public Set<Translation> getTranslations() {
        return translations;
    }

    public String getTranslatedName() {
        return this.getTranslations().stream()
                .filter(t -> Objects.equals(t.getLanguageCode(), Constants.DefaultLanguage))
                .findFirst()
                .map(Translation::getName)
                .orElseThrow();
    }

    Relevance(String uri, Set<Translation> translations) {
        this.id = URI.create(uri);
        this.translations = translations;
    }

    public static Optional<Relevance> getRelevance(URI id) {
        var relevances = Relevance.values();
        return Arrays.stream(relevances)
                .filter(relevance -> relevance.id.equals(id))
                .findFirst();
    }

    public static Relevance unsafeGetRelevance(URI id) {
        return getRelevance(id).orElseThrow(() -> new NotFoundException("Relevance", id));
    }

    public static List<Relevance> getRelevances() {
        return Arrays.asList(Relevance.values());
    }
}
