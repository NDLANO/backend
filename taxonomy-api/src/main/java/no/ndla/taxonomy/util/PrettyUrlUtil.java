/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import java.util.Optional;
import no.ndla.taxonomy.domain.LanguageField;
import no.ndla.taxonomy.domain.NodeType;
import org.jsoup.Jsoup;

public class PrettyUrlUtil {

    public static Optional<String> createPrettyUrl(
            Optional<LanguageField<String>> rootName,
            LanguageField<String> name,
            String language,
            String hash,
            NodeType nodeType) {
        return createPrettyUrl(
                rootName.map(lf -> lf.fromLanguage(language)), name.fromLanguage(language), hash, nodeType);
    }

    public static Optional<String> createPrettyUrl(
            Optional<String> rootName, String name, String hash, NodeType nodeType) {
        if (name == null || hash == null) return Optional.empty();
        StringBuilder builder = new StringBuilder();
        builder.append(nodeTypeMapping(nodeType));
        builder.append("/");
        rootName.ifPresent(rn -> {
            if (!rn.equals(name)) {
                buildUrlFragment(builder, cleanString(rootName.get()));
                builder.append("/");
            }
        });
        buildUrlFragment(builder, cleanString(name));
        builder.append(String.format("/%s", hash));

        return Optional.of(builder.toString());
    }

    public static String prettyName(String name) {
        var builder = new StringBuilder();
        buildUrlFragment(builder, cleanString(name));
        return builder.toString();
    }

    private static String nodeTypeMapping(NodeType nodeType) {
        return switch (nodeType) {
            case SUBJECT -> "/f";
            case TOPIC, CASE -> "/e";
            case RESOURCE -> "/r";
            case PROGRAMME -> "/utdanning";
            case NODE -> "";
        };
    }

    private static void buildUrlFragment(StringBuilder builder, String text) {
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append("-");
            }
            builder.append(words[i]);
        }
    }

    private static String cleanString(String name) {
        return Jsoup.parse(name)
                .text()
                .toLowerCase()
                .replaceAll("[.,!?¿()/«»\"”“'¡:`’#°π€%&|…]", "")
                .replaceAll("[æååäáǎāàãâ]", "a")
                .replaceAll("[øöôóòõǒ]", "o")
                .replaceAll("[ïíǐīìĩ]", "i")
                .replaceAll("[éèẽêë]", "e")
                .replaceAll("[šŝśş]", "s")
                .replaceAll("[čĉć]", "c")
                .replaceAll("[üùũúû]", "u")
                .replaceAll("[ðđ]", "d")
                .replaceAll("ğ", "g")
                .replaceAll("ñ", "n")
                .replaceAll("ž", "z")
                .replaceAll("ß", "ss")
                .replaceAll("₂", "2")
                .replaceAll("[—–−_：，]", "-")
                .trim();
    }

    public static String getHashFromPath(String title) {
        if (title.contains("__")) {
            return title.split("__")[1];
        }
        // titles with hash will have a /r/ or /e/ or /f/ in the path
        if (!title.contains("/r/") && !title.contains("/e/") && !title.contains("/f/")) {
            return "";
        }
        return title.substring(title.lastIndexOf('/') + 1);
    }
}
