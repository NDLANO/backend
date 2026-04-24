/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.service.UpdatableDto;

@Schema(requiredProperties = {"name"})
public class SubjectPostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If omitted, an id will be assigned automatically.",
            example = "urn:subject:1")
    public Optional<URI> id = Optional.empty();

    @JsonProperty
    @Schema(
            description = "ID of frontpage connected to this subject. Must be a valid URI, but preferably not a URL.",
            example = "urn:frontpage:1")
    public Optional<URI> contentUri = Optional.empty();

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The name of the subject",
            example = "Mathematics")
    public String name;

    @Override
    public Optional<URI> getId() {
        return id;
    }

    @Override
    public void apply(Node subject) {
        if (getId().isPresent()) subject.setPublicId(getId().get());
        subject.setContext(true);
        subject.setName(name);
        contentUri.ifPresent(subject::setContentUri);
    }
}
