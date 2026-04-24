/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class NodeConnection extends DomainEntity
        implements EntityWithMetadata, Comparable<NodeConnection>, SortableResourceConnection {
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Node parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    private Node child;

    @Column(name = "rank")
    private int rank;

    @Column(name = "is_primary")
    private boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column
    private Relevance relevance;

    @Column
    private boolean visible = true;

    @Type(JsonBinaryType.class)
    @Column(name = "grepcodes", columnDefinition = "jsonb")
    private Set<JsonGrepCode> grepcodes = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(name = "customfields", columnDefinition = "jsonb")
    private Map<String, String> customfields = new HashMap<>();

    @Column
    @CreationTimestamp
    private Instant created_at;

    @Column
    @UpdateTimestamp
    private Instant updated_at;

    @Enumerated(EnumType.STRING)
    @Column
    private NodeConnectionType connectionType = NodeConnectionType.BRANCH;

    public NodeConnection() {
        setPublicId(URI.create("urn:node-connection:" + UUID.randomUUID()));
    }

    public NodeConnection(NodeConnection nodeConnection) {
        this.rank = nodeConnection.rank;
        this.relevance = nodeConnection.relevance;
        this.parent = nodeConnection.parent;
        this.child = nodeConnection.child;
        this.connectionType = nodeConnection.connectionType;
        setPublicId(nodeConnection.getPublicId());
        setPrimary(nodeConnection.isPrimary().orElse(false));
        setMetadata(new Metadata(nodeConnection.getMetadata()));
    }

    // Special constructor for rankable test
    public NodeConnection(String uri, Relevance relevance, int rank) {
        setPublicId(URI.create(uri));
        this.relevance = relevance;
        this.rank = rank;
    }

    public static NodeConnection create(
            Node parent, Node child, Relevance relevance, NodeConnectionType connectionType) {
        return NodeConnection.create(parent, child, relevance, connectionType, true);
    }

    public static NodeConnection create(Node parent, Node child, Relevance relevance) {
        return NodeConnection.create(parent, child, relevance, NodeConnectionType.BRANCH, true);
    }

    public static NodeConnection create(Node parent, Node child, Relevance relevance, boolean isPrimary) {
        return NodeConnection.create(parent, child, relevance, NodeConnectionType.BRANCH, isPrimary);
    }

    public static NodeConnection create(
            Node parent, Node child, Relevance relevance, NodeConnectionType connectionType, boolean isPrimary) {
        if (child == null || parent == null) {
            throw new NullPointerException("Both parent and child must be present.");
        }
        if (relevance == null) {
            throw new NullPointerException("Relevance must be present.");
        }

        final var nodeConnection = new NodeConnection();
        nodeConnection.parent = parent;
        nodeConnection.child = child;
        nodeConnection.isPrimary = isPrimary;
        nodeConnection.relevance = relevance;
        nodeConnection.connectionType = connectionType;

        parent.addChildConnection(nodeConnection);
        child.addParentConnection(nodeConnection);

        return nodeConnection;
    }

    public void disassociate() {
        final var parent = this.parent;
        final var child = this.child;

        this.parent = null;
        this.child = null;

        if (parent != null) {
            parent.removeChildConnection(this);
        }
        if (child != null) {
            child.removeParentConnection(this);
        }
    }

    @Override
    public Optional<Node> getResource() {
        var child = getChild();
        var isResource = child.map(c -> c.getNodeType() == NodeType.RESOURCE).orElse(false);

        if (!isResource && child.isPresent())
            throw new IllegalStateException("Tried to getResource on a nodeConnection connected to a non-resource");

        return child;
    }

    public Optional<Node> getParent() {
        return Optional.ofNullable(parent);
    }

    public Optional<Node> getChild() {
        return Optional.ofNullable(child);
    }

    // only for publishing
    public void setParent(Node parent) {
        this.parent = parent;
    }

    // only for publishing
    public void setChild(Node child) {
        this.child = child;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @PreRemove
    public void preRemove() {
        disassociate();
    }

    public Optional<Boolean> isPrimary() {
        return Optional.of(this.isPrimary);
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(this);
    }

    @Override
    public Set<JsonGrepCode> getGrepCodes() {
        return this.grepcodes;
    }

    @Override
    public void setCustomField(String key, String value) {
        this.customfields.put(key, value);
    }

    @Override
    public void unsetCustomField(String key) {
        this.customfields.remove(key);
    }

    @Override
    public void setGrepCodes(Set<JsonGrepCode> codes) {
        this.grepcodes = codes;
    }

    @Override
    public void setCustomFields(Map<String, String> customFields) {
        this.customfields = customFields;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updated_at = updatedAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.created_at = createdAt;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public Instant getCreatedAt() {
        return this.created_at;
    }

    @Override
    public Instant getUpdatedAt() {
        return this.updated_at;
    }

    public NodeConnectionType getConnectionType() {
        return connectionType;
    }

    @Override
    public int compareTo(NodeConnection o) {
        return getPublicId().compareTo(o.getPublicId());
    }

    public Map<String, String> getCustomFields() {
        return this.customfields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeConnection that = (NodeConnection) o;
        return rank == that.rank
                && Objects.equals(parent, that.parent)
                && Objects.equals(child, that.child)
                && Objects.equals(relevance, that.relevance)
                && Objects.equals(connectionType, that.connectionType);
    }
}
