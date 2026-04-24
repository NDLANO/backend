/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.Optional;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.NodeType;
import org.junit.jupiter.api.Test;

class NodeDTOTest {

    @Test
    void defaultUrlNameUsesDefaultLanguageTranslation() {
        var node = new Node(NodeType.TOPIC);
        node.setName("English base name");
        node.addTranslation("Bokmal default name", "nb");

        var dto = new NodeDTO(
                Optional.empty(),
                Optional.empty(),
                node,
                NodeConnectionType.BRANCH,
                "en",
                Optional.empty(),
                false,
                false,
                true,
                false);

        assertEquals("bokmal-default-name", getField(dto, "defaultUrlName"));
    }
}
