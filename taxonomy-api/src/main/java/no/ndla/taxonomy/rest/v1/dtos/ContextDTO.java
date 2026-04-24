/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

@Schema(
        name = "Context",
        requiredProperties = {"id", "name", "path"})
public class ContextDTO {
    public URI id;
    public String path;
    public String name;

    public ContextDTO(URI id, String name, String path) {
        this.id = id;
        this.name = name;
        this.path = path;
    }

    public URI getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
