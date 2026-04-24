/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import org.springframework.stereotype.Component;

/**
 * Used to ensure that queries for old style URLS of various kinds (ndla.no/node/xxx) are transformed to the same
 * format, used in lookup.
 */
@Component
public class OldUrlCanonifier {

    private static final String[] KNOWN_NODE_PREFIXES = new String[] {
        "printpdf", "easyreader", "h5p/embed", "h5pcontent", "aktualitet", "package", "fagstoff", "oppgave", "print"
    };
    private static final String[] KNOWN_NODE_SUFFIXES = new String[] {"/menu", "/oembed", "/download", "/lightbox"};

    public String canonify(String oldUrl) {
        oldUrl = replaceKnownNodePrefixes(oldUrl);
        oldUrl = discardKnownNodeSuffixes(oldUrl);
        String nodeId = "";
        String fagId = "";
        for (String token : tokenize(oldUrl)) {
            if (!token.contains("=")) {
                int nodeStartsAt = findNodeStartsAt(token);
                nodeId = token.substring(nodeStartsAt);
            } else if (token.contains("fag=")) {
                int start = token.indexOf("fag=");
                int ampersandIndex = token.indexOf("&");
                int end = ampersandIndex > start ? ampersandIndex : token.length();
                fagId = "?" + token.substring(start, end);
            }
        }
        return "ndla.no" + nodeId + fagId;
    }

    private String discardKnownNodeSuffixes(String oldUrl) {
        for (String suffix : KNOWN_NODE_SUFFIXES) {
            if (oldUrl.contains(suffix)) {
                int start = oldUrl.indexOf(suffix);
                int indexOfSlashAfter = oldUrl.indexOf("/", start + 1);
                int indexOfQuestionMark = oldUrl.indexOf("?", start);
                String partToRemove;
                if (indexOfSlashAfter != -1) {
                    partToRemove = oldUrl.substring(start, indexOfSlashAfter + 1);
                } else if (indexOfQuestionMark != -1) {
                    partToRemove = oldUrl.substring(start, indexOfQuestionMark);
                } else partToRemove = suffix;
                return oldUrl.replace(partToRemove, "");
            }
        }
        return oldUrl;
    }

    private String replaceKnownNodePrefixes(String oldUrl) {
        for (String prefix : KNOWN_NODE_PREFIXES) {
            if (oldUrl.contains(prefix)) {
                return oldUrl.replace(prefix, "node");
            }
        }
        return oldUrl;
    }

    private int findNodeStartsAt(String token) {
        return token.substring(0, token.lastIndexOf("/")).lastIndexOf("/");
    }

    private String[] tokenize(String oldUrl) {
        return oldUrl.split("\\?");
    }
}
