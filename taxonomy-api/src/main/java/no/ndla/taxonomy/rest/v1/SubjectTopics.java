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
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicDTO;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicPOST;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicPUT;
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
@RequestMapping(path = {"/v1/subject-topics", "/v1/subject-topics/"})
public class SubjectTopics {
    private final NodeConnectionService connectionService;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    public SubjectTopics(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            NodeConnectionService connectionService) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all connections between subjects and topics")
    @Transactional(readOnly = true)
    public List<SubjectTopicDTO> getAllSubjectTopics() {
        final List<SubjectTopicDTO> listToReturn = new ArrayList<>();
        var ids = nodeConnectionRepository.findAllIds();
        final var counter = new AtomicInteger();
        ids.stream()
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .forEach(idChunk -> {
                    final var connections = nodeConnectionRepository.findByIds(idChunk);
                    var dtos = connections.stream().map(SubjectTopicDTO::new).toList();
                    listToReturn.addAll(dtos);
                });

        return listToReturn;
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all connections between subjects and topics paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<SubjectTopicDTO> getSubjectTopicPage(
            @Parameter(description = "The page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Size of page to fetch") @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {
        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page - 1, pageSize));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(SubjectTopicDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page, pageSize, contents);
    }

    @Deprecated
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific connection between a subject and a topic")
    @Transactional(readOnly = true)
    public SubjectTopicDTO getSubjectTopic(@PathVariable("id") URI id) {
        NodeConnection nodeConnection = nodeConnectionRepository.getByPublicId(id);
        return new SubjectTopicDTO(nodeConnection);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Adds a new topic to a subject",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createSubjectTopic(
            @Parameter(name = "command", description = "The subject and topic getting connected.") @RequestBody
                    SubjectTopicPOST command) {
        var subject = nodeRepository.getByPublicId(command.subjectid);
        var topic = nodeRepository.getByPublicId(command.topicid);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        var rank = command.rank.orElse(null);
        final var nodeConnection = connectionService.connectParentChild(
                subject, topic, relevance, rank, Optional.empty(), NodeConnectionType.BRANCH);
        var location = URI.create("/subject-topics/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @Deprecated
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Removes a topic from a subject",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteSubjectTopic(@PathVariable("id") URI id) {
        connectionService.disconnectParentChildConnection(nodeConnectionRepository.getByPublicId(id));
    }

    @Deprecated
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Updates a connection between subject and topic",
            description = "Use to update which subject is primary to a topic or to change sorting order.",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateSubjectTopic(
            @PathVariable("id") URI id,
            @Parameter(name = "connection", description = "updated subject/topic connection") @RequestBody
                    SubjectTopicPUT command) {
        var nodeConnection = nodeConnectionRepository.getByPublicId(id);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));

        connectionService.updateParentChild(nodeConnection, relevance, command.rank, Optional.empty());
    }
}
