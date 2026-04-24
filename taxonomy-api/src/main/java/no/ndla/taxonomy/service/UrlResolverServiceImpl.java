/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.TaxonomyContext;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.UrlMappingRepository;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.util.PrettyUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UrlResolverServiceImpl implements UrlResolverService {

    private final OldUrlCanonifier canonifier;

    private final NodeRepository nodeRepository;
    private final UrlMappingRepository urlMappingRepository;

    @Autowired
    public UrlResolverServiceImpl(
            UrlMappingRepository urlMappingRepository,
            NodeRepository nodeRepository,
            OldUrlCanonifier oldUrlCanonifier) {
        this.nodeRepository = nodeRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.canonifier = oldUrlCanonifier;
    }

    @Override
    public Optional<String> resolveOldUrl(String oldUrl) {
        final var results = getCachedUrlOldRig(oldUrl);
        if (!results.isEmpty()) {
            final var result = results.getFirst();
            final var allPaths = getAllPaths(result.getPublic_id());

            if (result.getSubject_id() != null) {
                final var shortestPath = findshortestPathStartingWith(result.getSubject_id(), allPaths);

                if (shortestPath.isPresent()) {
                    return shortestPath;
                }
            }

            return getPrimaryPath(result.getPublic_id());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> findshortestPathStartingWith(URI subject_id, List<String> allPaths) {
        String subject = "/" + subject_id.toString().split("urn:")[1];
        allPaths.sort(Comparator.comparing(String::length));
        return allPaths.stream().filter(path -> path.startsWith(subject)).findFirst();
    }

    private List<String> getAllPaths(URI publicId) {
        try {
            return getEntityFromPublicId(publicId).stream()
                    .flatMap(x -> x.getAllPaths().stream())
                    .collect(Collectors.toList());
        } catch (InvalidArgumentServiceException e) {
            return List.of();
        }
    }

    private Optional<String> getPrimaryPath(URI publicId) {
        try {
            return getEntityFromPublicId(publicId).stream()
                    .map(Node::getPrimaryPath)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        } catch (InvalidArgumentServiceException e) {
            return Optional.empty();
        }
    }

    private List<UrlMapping> getCachedUrlOldRig(String oldUrl) {
        String canonicalUrl = canonifier.canonify(oldUrl);
        String queryUrl = canonicalUrl + "%";

        return urlMappingRepository.findAllByOldUrlLike(queryUrl).stream()
                // the LIKE query may match node IDs that __start with__ the same node ID as in old
                // url
                // e.g. oldUrl /node/54 should not match /node/54321 - therefore we add only if IDs
                // match
                .filter(urlMapping -> urlMapping.getOldUrl() != null)
                .filter(mapping -> getNodeId(canonicalUrl).equals(getNodeId(mapping.getOldUrl())))
                .collect(Collectors.toList());
    }

    private Optional<String> getNodeId(String url) {
        if (url != null) {
            if (url.contains("?") && url.contains("/")) {
                return Optional.of(url.substring(url.lastIndexOf("/"), url.indexOf("?")));
            } else if (url.contains("/")) {
                return Optional.of(url.substring(url.lastIndexOf("/")));
            }
        }
        return Optional.empty();
    }

    /**
     * put old url into URL_MAP
     *
     * @param oldUrl
     *            url to put
     * @param nodeId
     *            nodeID to be associated with this URL
     * @param subjectId
     *            subjectID to be associated with this URL (optional)
     *
     * @throws NodeIdNotFoundExeption
     *             if node ide not found in taxonomy
     */
    @Transactional
    @Override
    public void putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption {
        oldUrl = canonifier.canonify(oldUrl);
        if (getAllPaths(nodeId).isEmpty())
            throw new NodeIdNotFoundExeption("Node id not found in taxonomy for " + oldUrl);
        if (getCachedUrlOldRig(oldUrl).isEmpty()) {
            final var urlMapping = new UrlMapping();
            urlMapping.setOldUrl(oldUrl);
            urlMapping.setPublic_id(nodeId.toString());
            urlMapping.setSubject_id(subjectId);
            urlMappingRepository.save(urlMapping);
        } else {
            urlMappingRepository.findAllByOldUrl(oldUrl).forEach(urlMapping -> {
                urlMapping.setPublic_id(nodeId.toString());
                urlMapping.setSubject_id(subjectId.toString());
                urlMappingRepository.save(urlMapping);
            });
        }
    }

    @Override
    public Optional<ResolvedUrl> resolveUrl(String path, String language) {
        try {
            final var resolvedPathComponents = resolveEntitiesFromPath(path);
            final var normalizedPath =
                    resolvedPathComponents.stream().map(Node::getPathPart).collect(Collectors.joining());
            final var leafNode = resolvedPathComponents.getLast();
            Optional<TaxonomyContext> context = leafNode.getContexts().stream()
                    .filter(ctx -> ctx.path().equals(normalizedPath))
                    .findFirst();

            final var resolvedUrl = new ResolvedUrl();
            // Pick either the context matching path or the primary context
            context.or(() -> leafNode.pickContext(
                            Optional.empty(), Optional.empty(), Optional.empty(), NodeConnectionType.BRANCH, Set.of()))
                    .map(Optional::of)
                    .orElseThrow(() -> new NotFoundServiceException("No context found for path"))
                    .ifPresent(ctx -> {
                        resolvedUrl.setExactMatch(context.isPresent());
                        resolvedUrl.setContentUri(leafNode.getContentUri());
                        resolvedUrl.setId(URI.create(ctx.publicId()));
                        resolvedUrl.setParents(ctx.parentIds().stream()
                                .map(URI::create)
                                .toList()
                                .reversed());
                        resolvedUrl.setName(ctx.name().fromLanguage(language));
                        resolvedUrl.setPath(ctx.path());
                        resolvedUrl.setUrl(PrettyUrlUtil.createPrettyUrl(
                                        Optional.ofNullable(ctx.rootName()),
                                        ctx.name(),
                                        language,
                                        ctx.contextId(),
                                        ctx.nodeType())
                                .orElse(ctx.path()));
                    });
            return Optional.of(resolvedUrl);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<Node> resolveEntitiesFromPath(String path) {
        final var returnedList = new ArrayList<Node>();
        final var pathParts = path.split("/+");
        for (String pathPart : pathParts) {
            // Ignore empty parts of the path, including when having leading and/or trailing slashes
            // and multiple slashes
            if (pathPart.isEmpty()) {
                continue;
            }

            try {
                final var resolved = getEntityFromPublicId(URI.create("urn:" + pathPart))
                        .orElseThrow(() ->
                                new NotFoundServiceException("Element with Id " + pathPart + " could not be found"));
                returnedList.add(resolved);
            } catch (Exception e) {
                // Do nothing, just skip the part of the path that could not be resolved
            }
        }

        return returnedList;
    }

    private Optional<Node> getEntityFromPublicId(URI publicId) {
        final var publicIdUrnPart = publicId.getSchemeSpecificPart();
        if (!"urn".equals(publicId.getScheme()) || publicIdUrnPart == null) {
            throw new InvalidArgumentServiceException("No valid URN provided");
        }

        return nodeRepository.findFirstByPublicId(publicId);
    }
}
