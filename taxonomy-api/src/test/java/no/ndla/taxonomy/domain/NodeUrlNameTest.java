/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class NodeUrlNameTest {

    @Test
    void translatedPrettyNamesSkipsNullTranslationNames() {
        var node = new Node(NodeType.TOPIC);
        node.setName("Default Name");
        node.addTranslation(new JsonTranslation(null, "nn"));

        assertEquals(Set.of("default-name"), node.translatedPrettyNames());
    }
}
