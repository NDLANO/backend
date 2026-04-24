/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ndla.taxonomy.config.Constants;
import org.apache.commons.lang3.SerializationUtils;

public class LanguageField<V> extends HashMap<String, V> {
    public LanguageField() {}

    public static LanguageField<String> fromNode(DomainObject node) {
        var languageField = new LanguageField<String>();
        var defaultName = Optional.ofNullable(node.getName()).orElse("");
        languageField.put(Constants.DefaultLanguage, defaultName);

        node.getTranslations().forEach(nt -> languageField.put(nt.getLanguageCode(), nt.getName()));

        return languageField;
    }

    public static LanguageField<String> fromRelevance(Relevance relevance) {
        var languageField = new LanguageField<String>();
        var defaultName = relevance.getTranslations().stream()
                .filter(t -> Objects.equals(t.getLanguageCode(), "nb"))
                .findFirst()
                .map(Translation::getName)
                .orElse("");
        languageField.put(Constants.DefaultLanguage, defaultName);

        relevance.getTranslations().forEach(nt -> languageField.put(nt.getLanguageCode(), nt.getName()));

        return languageField;
    }

    public static LanguageField<List<String>> listFromNode(DomainObject node) {
        var breadcrumbs = new LanguageField<List<String>>();
        var langs = node.getTranslations().stream()
                .map(Translation::getLanguageCode)
                .collect(Collectors.toSet());
        langs.add(Constants.DefaultLanguage);
        for (var lang : langs) {
            var crumbs = new ArrayList<String>();
            crumbs.add(node.getTranslatedName(lang));
            breadcrumbs.put(lang, crumbs);
        }
        return breadcrumbs;
    }

    /**
     * Returns accumulated set of language fields. All additional languages from languageField are appended so all
     * languageLists have the same number of elements. If a language variant is not present, default value is used.
     */
    public static LanguageField<List<String>> listFromLists(
            LanguageField<List<String>> listLanguageField, LanguageField<String> languageField) {
        var breadcrumbs = SerializationUtils.clone(listLanguageField);
        var languages = Stream.concat(listLanguageField.keySet().stream(), languageField.keySet().stream())
                .collect(Collectors.toSet());
        var defaultValue = languageField.get(Constants.DefaultLanguage);
        languages.forEach(lang -> {
            var crumbs = breadcrumbs.computeIfAbsent(
                    lang, k -> listLanguageField.getOrDefault(Constants.DefaultLanguage, new ArrayList<>()));
            crumbs.add(languageField.getOrDefault(lang, defaultValue));
        });
        return breadcrumbs;
    }

    public V fromLanguage(String language) {
        return this.getOrDefault(language, this.get(Constants.DefaultLanguage));
    }
}
