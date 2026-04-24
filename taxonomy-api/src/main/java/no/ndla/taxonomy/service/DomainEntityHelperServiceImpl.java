/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    public DomainEntityHelperServiceImpl(
            NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DomainEntity getEntityByPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
            case "subject", "topic", "node", "resource", "programme" -> {
                return nodeRepository.findFirstByPublicId(publicId).orElse(null);
            }
            case "node-connection", "subject-topic", "topic-subtopic", "node-resource", "topic-resource" -> {
                return nodeConnectionRepository.findByPublicId(publicId);
            }
        }
        throw new NotFoundServiceException("Entity of type not found");
    }
}
