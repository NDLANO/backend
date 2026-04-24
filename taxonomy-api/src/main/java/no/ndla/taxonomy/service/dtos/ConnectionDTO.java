/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import no.ndla.taxonomy.domain.NodeConnection;

/** */
@Schema(
        name = "Connection",
        requiredProperties = {"connectionId", "targetId", "paths", "type", "isPrimary"})
public class ConnectionDTO {

    @JsonProperty
    @Schema(description = "The id of the subject-topic or topic-subtopic connection", example = "urn:subject-topic:1")
    private URI connectionId;

    @JsonProperty
    @Schema(description = "The id of the connected subject or topic", example = "urn:subject:1")
    private URI targetId;

    @JsonProperty
    @Schema(
            description = "The path part of the url for the subject or subtopic connected to this topic",
            example = "/subject:1/topic:1",
            deprecated = true)
    private Set<String> paths = new HashSet<>();

    @JsonProperty
    @Schema(description = "The type of connection (parent, child, referrer or target")
    private String type;

    @JsonProperty
    @Schema(
            description = "True if owned by this topic, false if it has its primary connection elsewhere",
            example = "true")
    private Boolean isPrimary;

    public ConnectionDTO() {}

    private ConnectionDTO(NodeConnection connection, boolean isParentConnection) {
        this.connectionId = connection.getPublicId();
        this.isPrimary = connection.isPrimary().orElse(false);

        final var connectedObject = isParentConnection ? connection.getParent() : connection.getChild();

        connectedObject.ifPresent(connected -> {
            this.targetId = connected.getPublicId();
            this.paths = Set.of();
        });
        this.type = isParentConnection
                ? connection.getConnectionType().getFrom()
                : connection.getConnectionType().getTo();
    }

    public static ConnectionDTO parentConnection(NodeConnection connection) {
        return new ConnectionDTO(connection, true);
    }

    public static ConnectionDTO childConnection(NodeConnection connection) {
        return new ConnectionDTO(connection, false);
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public URI getTargetId() {
        return targetId;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public String getType() {
        return type;
    }

    @Deprecated
    public Boolean getPrimary() {
        return isPrimary;
    }

    @JsonProperty("isPrimary")
    public Boolean isPrimary() {
        return isPrimary;
    }
}
