/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicDTO;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicPOST;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicPUT;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.NodeConnectionService;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/topic-subtopics", "/v1/topic-subtopics/"})
public class TopicSubtopics {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;

    public TopicSubtopics(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            NodeConnectionService connectionService) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all connections between topics and subtopics")
    @Transactional(readOnly = true)
    public List<TopicSubtopicDTO> getAllTopicSubtopics() {
        final List<TopicSubtopicDTO> listToReturn = new ArrayList<>();
        var ids = nodeConnectionRepository.findAllIds();
        final var counter = new AtomicInteger();
        ids.stream()
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .forEach(idChunk -> {
                    final var connections = nodeConnectionRepository.findByIds(idChunk);
                    var dtos = connections.stream().map(TopicSubtopicDTO::new).toList();
                    listToReturn.addAll(dtos);
                });

        return listToReturn;
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all connections between topics and subtopics paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<TopicSubtopicDTO> getTopicSubtopicPage(
            @Parameter(description = "The page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Size of page to fetch") @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {
        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page - 1, pageSize));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(TopicSubtopicDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page, pageSize, contents);
    }

    @Deprecated
    @GetMapping("/{id}")
    @Operation(summary = "Gets a single connection between a topic and a subtopic")
    @Transactional(readOnly = true)
    public TopicSubtopicDTO getTopicSubtopic(@PathVariable("id") URI id) {
        NodeConnection topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        return new TopicSubtopicDTO(topicSubtopic);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Adds a subtopic to a topic",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createTopicSubtopic(
            @Parameter(name = "connection", description = "The new connection") @RequestBody
                    TopicSubtopicPOST command) {
        Node topic = nodeRepository.getByPublicId(command.topicid);
        Node subtopic = nodeRepository.getByPublicId(command.subtopicid);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        var rank = command.rank.orElse(null);

        final var topicSubtopic = connectionService.connectParentChild(
                topic, subtopic, relevance, rank, Optional.empty(), NodeConnectionType.BRANCH);

        URI location = URI.create("/topic-subtopics/" + topicSubtopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @Deprecated
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Removes a connection between a topic and a subtopic",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteTopicSubtopic(@PathVariable("id") URI id) {
        connectionService.disconnectParentChildConnection(nodeConnectionRepository.getByPublicId(id));
    }

    @Deprecated
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Updates a connection between a topic and a subtopic",
            description = "Use to update which topic is primary to a subtopic or to alter sorting order",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateTopicSubtopic(
            @PathVariable("id") URI id,
            @Parameter(name = "connection", description = "The updated connection") @RequestBody
                    TopicSubtopicPUT command) {
        final var topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        connectionService.updateParentChild(topicSubtopic, relevance, command.rank, Optional.empty());
    }
}
