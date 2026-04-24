/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;

public interface UrlResolverService {
    /**
     * @param oldUrl
     *            url previously imported into taxonomy with taxonomy-import
     *
     * @return return a resolved URL or null
     */
    Optional<String> resolveOldUrl(String oldUrl);

    void putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption;

    Optional<ResolvedUrl> resolveUrl(String path, String language);

    class NodeIdNotFoundExeption extends Exception {
        public NodeIdNotFoundExeption(String message) {
            super(message);
        }
    }
}
