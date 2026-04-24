/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum NodeType {
    NODE("node"),
    SUBJECT("subject"),
    TOPIC("topic"),
    CASE("case"),
    RESOURCE("resource"),
    PROGRAMME("programme");

    private final String name;

    NodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
