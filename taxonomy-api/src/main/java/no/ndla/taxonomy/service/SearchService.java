/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final NodeRepository nodeRepository;

    public SearchService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public Specification<Node> nodeHasOneOfNodeType(List<NodeType> nodeType) {
        return (root, query, builder) -> root.get("nodeType").in(nodeType);
    }

    public SearchResultDTO<NodeDTO> searchByNodeType(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<List<String>> contentUris,
            String language,
            boolean includeContexts,
            boolean filterProgrammes,
            int pageSize,
            int page,
            Optional<List<NodeType>> nodeType,
            Optional<Map<String, String>> customfieldsFilter,
            Optional<URI> rootId,
            Optional<URI> parentId) {
        Optional<ExtraSpecification> nodeSpecLambda = nodeType.map(nt -> (s -> s.and(nodeHasOneOfNodeType(nt))));
        return this.search(
                query,
                ids,
                contentUris,
                language,
                includeContexts,
                filterProgrammes,
                pageSize,
                page,
                nodeSpecLambda,
                customfieldsFilter,
                rootId,
                parentId);
    }

    private Specification<Node> base() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("id"));
    }

    private Specification<Node> withNameLike(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private Specification<Node> withPublicIdsIn(List<URI> ids) {
        return (root, query, criteriaBuilder) -> root.get("publicId").in(ids);
    }

    private Specification<Node> withContentUriIn(List<URI> contentUris) {
        return (root, query, criteriaBuilder) -> root.get("contentUri").in(contentUris);
    }

    private Specification<Node> withKeyAndValue(String key, String value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get("customfields"),
                        criteriaBuilder.literal(key)),
                value);
    }

    public SearchResultDTO<NodeDTO> search(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<List<String>> contentUris,
            String language,
            boolean includeContexts,
            boolean filterProgrammes,
            int pageSize,
            int page,
            Optional<ExtraSpecification> applySpecLambda,
            Optional<Map<String, String>> customFieldFilters,
            Optional<URI> rootId,
            Optional<URI> parentId) {

        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page - 1, pageSize);
        Specification<Node> spec = base();

        if (query.isPresent()) {
            spec = spec.and(withNameLike(query.get()));
        }

        spec = applyIdFilters(ids, spec);
        spec = applyContentUriFilters(contentUris, spec);

        if (applySpecLambda.isPresent()) {
            spec = applySpecLambda.get().applySpecification(spec);
        }

        spec = applyCustomFieldsFilters(spec, customFieldFilters);

        var fetched = nodeRepository.findAll(spec, pageRequest);

        var rootNode = rootId.flatMap(nodeRepository::findFirstByPublicId);
        var parentNode = parentId.flatMap(nodeRepository::findFirstByPublicId);

        var languageCode = language != null ? language : "";
        var dtos = fetched.stream()
                .map(r -> new NodeDTO(
                        rootNode,
                        parentNode,
                        r,
                        NodeConnectionType.BRANCH,
                        languageCode,
                        Optional.empty(),
                        includeContexts,
                        filterProgrammes,
                        false,
                        false))
                .collect(Collectors.toList());

        return new SearchResultDTO<>(fetched.getTotalElements(), page, pageSize, dtos);
    }

    private Specification<Node> applyContentUriFilters(Optional<List<String>> contentUris, Specification<Node> spec) {
        if (contentUris.isPresent()) {
            List<URI> urisToPass = contentUris.get().stream()
                    .flatMap(id -> {
                        try {
                            return Optional.of(new URI(id)).stream();
                        } catch (URISyntaxException ignored) {
                            /* ignore invalid urls sent by user */
                        }
                        return Optional.<URI>empty().stream();
                    })
                    .collect(Collectors.toList());

            if (!urisToPass.isEmpty()) spec = spec.and(withContentUriIn(urisToPass));
        }
        return spec;
    }

    private Specification<Node> applyIdFilters(Optional<List<String>> ids, Specification<Node> spec) {
        if (ids.isPresent()) {
            List<URI> urisToPass = ids.get().stream()
                    .flatMap(id -> {
                        try {
                            return Optional.of(new URI(id)).stream();
                        } catch (URISyntaxException ignored) {
                            /* ignore invalid urls sent by user */
                        }
                        return Optional.<URI>empty().stream();
                    })
                    .collect(Collectors.toList());

            if (!urisToPass.isEmpty()) spec = spec.and(withPublicIdsIn(urisToPass));
        }
        return spec;
    }

    private Specification<Node> applyCustomFieldsFilters(
            Specification<Node> spec, Optional<Map<String, String>> metadataFilters) {
        if (metadataFilters.isPresent()) {
            Specification<Node> filterSpec = null;
            for (var entry : metadataFilters.get().entrySet()) {
                if (filterSpec != null) {
                    filterSpec = filterSpec.or(withKeyAndValue(entry.getKey(), entry.getValue()));
                } else {
                    filterSpec = withKeyAndValue(entry.getKey(), entry.getValue());
                }
            }
            if (filterSpec != null) {
                return spec.and(filterSpec);
            }
        }
        return spec;
    }
}
