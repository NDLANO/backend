/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import no.ndla.taxonomy.service.ContextUpdaterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class Builder {
    private final EntityManager entityManager;
    private final ContextUpdaterService contextUpdaterService;
    private final Map<String, VersionBuilder> versions = new HashMap<>();
    private final Map<String, ResourceTypeBuilder> resourceTypes = new HashMap<>();
    private final Map<String, NodeBuilder> nodes = new HashMap<>();
    private final Map<String, UrlMappingBuilder> cachedUrlOldRigBuilders = new HashMap<>();
    private int keyCounter = 0;

    public Builder(EntityManager entityManager, ContextUpdaterService contextUpdaterService) {
        this.entityManager = entityManager;
        this.contextUpdaterService = contextUpdaterService;
    }

    private String createKey() {
        return "DefaultKey:" + keyCounter++;
    }

    public Version version() {
        return version(null, null);
    }

    public Version version(String key) {
        return version(key, null);
    }

    public Version version(Consumer<VersionBuilder> consumer) {
        return version(null, consumer);
    }

    public Version version(String key, Consumer<VersionBuilder> consumer) {
        VersionBuilder builder = getVersionBuilder(key);
        if (null != consumer) consumer.accept(builder);

        entityManager.persist(builder.version);

        return builder.version;
    }

    public ResourceType resourceType(String key) {
        return resourceType(key, null);
    }

    public ResourceType resourceType(Consumer<ResourceTypeBuilder> consumer) {
        return resourceType(null, consumer);
    }

    public ResourceType resourceType(String key, Consumer<ResourceTypeBuilder> consumer) {
        ResourceTypeBuilder resourceType = getResourceTypeBuilder(key);
        if (null != consumer) consumer.accept(resourceType);
        return resourceType.resourceType;
    }

    private VersionBuilder getVersionBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        versions.putIfAbsent(key, new VersionBuilder());
        return versions.get(key);
    }

    private NodeBuilder getNodeBuilder(String key, NodeType nodeType) {
        if (key == null) {
            key = createKey();
        }
        if (nodeType == null) {
            nodeType = NodeType.NODE;
        }
        nodes.putIfAbsent(key, new NodeBuilder(nodeType));
        return nodes.get(key);
    }

    private ResourceTypeBuilder getResourceTypeBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        resourceTypes.putIfAbsent(key, new ResourceTypeBuilder());
        return resourceTypes.get(key);
    }

    private NodeBuilder getResourceBuilder(String key) {
        return getNodeBuilder(key, NodeType.RESOURCE);
    }

    public Node node() {
        return node(null, null, null);
    }

    public Node node(String key) {
        return node(key, null, null);
    }

    public Node node(NodeType nodeType) {
        return node(null, nodeType, null);
    }

    public Node node(Consumer<NodeBuilder> consumer) {
        return node(null, null, consumer);
    }

    public Node node(NodeType nodeType, Consumer<NodeBuilder> consumer) {
        return node(null, nodeType, consumer);
    }

    public Node node(String key, NodeType nodeType, Consumer<NodeBuilder> consumer) {
        NodeBuilder node = getNodeBuilder(key, nodeType);
        if (null != consumer) consumer.accept(node);

        entityManager.persist(node.node);
        contextUpdaterService.updateContexts(node.node);

        return node.node;
    }

    private UrlMappingBuilder getUrlMappingBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        cachedUrlOldRigBuilders.putIfAbsent(key, new UrlMappingBuilder());
        return cachedUrlOldRigBuilders.get(key);
    }

    @Transactional
    public static class VersionBuilder {
        private final Version version;

        public VersionBuilder() {
            this.version = new Version();
        }

        public VersionBuilder publicId(URI publicId) {
            version.setPublicId(publicId);
            return this;
        }

        public VersionBuilder type(VersionType versionType) {
            version.setVersionType(versionType);
            return this;
        }

        public VersionBuilder locked(Boolean locked) {
            version.setLocked(locked);
            return this;
        }
    }

    public static class JsonTranslationBuilder {
        private final JsonTranslation translation;

        public JsonTranslationBuilder(JsonTranslation nodeTranslation) {
            this.translation = nodeTranslation;
        }

        public JsonTranslationBuilder name(String name) {
            translation.setName(name);
            return this;
        }
    }

    @Transactional
    public static class UrlMappingBuilder {
        private final UrlMapping urlMapping;

        public UrlMappingBuilder() {
            urlMapping = new UrlMapping();
        }

        public UrlMappingBuilder oldUrl(String p) {
            urlMapping.setOldUrl(p);
            return this;
        }

        public UrlMappingBuilder public_id(String p) {
            urlMapping.setPublic_id(p);
            return this;
        }

        public UrlMappingBuilder subject_id(String s) {
            urlMapping.setSubject_id(s);
            return this;
        }
    }

    @Transactional
    public class ResourceTypeBuilder {
        private final ResourceType resourceType;

        public ResourceTypeBuilder() {
            resourceType = new ResourceType();
            entityManager.persist(resourceType);
        }

        public ResourceTypeBuilder name(String name) {
            resourceType.name(name);
            return this;
        }

        public ResourceTypeBuilder translation(String name, String languageCode) {
            resourceType.addTranslation(name, languageCode);
            entityManager.persist(resourceType);
            return this;
        }

        public ResourceTypeBuilder translation(String languageCode, Consumer<JsonTranslationBuilder> consumer) {
            var nodeTranslation = resourceType.addTranslation("", languageCode);
            var builder = new JsonTranslationBuilder(nodeTranslation);
            consumer.accept(builder);
            entityManager.persist(resourceType);
            return this;
        }

        public ResourceTypeBuilder subtype(Consumer<ResourceTypeBuilder> consumer) {
            return subtype(null, consumer);
        }

        public ResourceTypeBuilder subtype(String key, Consumer<ResourceTypeBuilder> consumer) {
            ResourceTypeBuilder resourceTypeBuilder = getResourceTypeBuilder(key);
            if (null != consumer) consumer.accept(resourceTypeBuilder);
            subtype(resourceTypeBuilder.resourceType);
            return this;
        }

        public ResourceTypeBuilder subtype(ResourceType subtype) {
            subtype.setParent(resourceType);
            return this;
        }

        public ResourceTypeBuilder publicId(String id) {
            resourceType.setPublicId(URI.create(id));
            return this;
        }

        public ResourceTypeBuilder order(int order) {
            resourceType.setOrder(order);
            return this;
        }
    }

    public UrlMapping urlMapping(Consumer<UrlMappingBuilder> consumer) {
        return urlMapping(null, consumer);
    }

    public UrlMapping urlMapping(String key, Consumer<UrlMappingBuilder> consumer) {
        UrlMappingBuilder urlMapping = getUrlMappingBuilder(key);
        if (null != consumer) consumer.accept(urlMapping);
        return urlMapping.urlMapping;
    }

    @Transactional
    public class NodeBuilder {
        private final Node node;

        public NodeBuilder(NodeType nodeType) {
            node = new Node(nodeType);
            entityManager.persist(node);
        }

        public NodeBuilder nodeType(NodeType nodeType) {
            node.setNodeType(nodeType);
            contextUpdaterService.updateContexts(node);
            return this;
        }

        public NodeBuilder name(String name) {
            node.setName(name);
            return this;
        }

        public NodeBuilder qualityEvaluation(Grade grade) {
            node.setQualityEvaluation(grade);
            return this;
        }

        public NodeBuilder resourceType(ResourceType resourceType) {
            entityManager.persist(node.addResourceType(resourceType));
            return this;
        }

        public NodeBuilder resourceType(Consumer<ResourceTypeBuilder> consumer) {
            return resourceType(null, consumer);
        }

        public NodeBuilder resourceType(String resourceTypeKey, Consumer<ResourceTypeBuilder> consumer) {
            ResourceTypeBuilder resourceTypeBuilder = getResourceTypeBuilder(resourceTypeKey);
            if (null != consumer) consumer.accept(resourceTypeBuilder);
            return resourceType(resourceTypeBuilder.resourceType);
        }

        public NodeBuilder resourceType(String resourceTypeKey) {
            return resourceType(resourceTypeKey, null);
        }

        public NodeBuilder contentUri(String contentUri) {
            return contentUri(URI.create(contentUri));
        }

        public NodeBuilder contentUri(URI contentUri) {
            node.setContentUri(contentUri);
            return this;
        }

        public NodeBuilder publicId(String id) {
            node.setPublicId(URI.create(id));

            contextUpdaterService.updateContexts(node);
            return this;
        }

        public NodeBuilder isContext(boolean b) {
            node.setContext(b);

            contextUpdaterService.updateContexts(node);
            return this;
        }

        public NodeBuilder isVisible(boolean visible) {
            node.getMetadata().setVisible(visible);
            return this;
        }

        public NodeBuilder grepCode(String code) {
            this.node.addGrepCode(code);
            entityManager.persist(this.node);
            return this;
        }

        public NodeBuilder customField(String key, String value) {
            this.node.setCustomField(key, value);
            entityManager.persist(this.node);
            return this;
        }

        public NodeBuilder child(String nodeKey) {
            return child(nodeKey, null, null);
        }

        public NodeBuilder child(String nodeKey, NodeType nodeType) {
            return child(nodeKey, nodeType, null);
        }

        public NodeBuilder child(Consumer<NodeBuilder> consumer) {
            return child(null, null, consumer);
        }

        public NodeBuilder child(NodeType nodeType, Consumer<NodeBuilder> consumer) {
            return child(null, nodeType, consumer);
        }

        public NodeBuilder child(String key, NodeType nodeType, Consumer<NodeBuilder> consumer) {
            NodeBuilder nodeBuilder = getNodeBuilder(key, nodeType);
            if (null != consumer) consumer.accept(nodeBuilder);
            child(nodeBuilder.node);
            return this;
        }

        public NodeBuilder child(Node child) {
            entityManager.persist(NodeConnection.create(node, child, Relevance.CORE));
            contextUpdaterService.updateContexts(child);

            return this;
        }

        public NodeBuilder link(Node other) {
            entityManager.persist(NodeConnection.create(node, other, Relevance.CORE, NodeConnectionType.LINK));
            contextUpdaterService.updateContexts(other);
            return this;
        }

        public NodeBuilder resource() {
            return resource(null, null);
        }

        public NodeBuilder resource(String resourceKey) {
            return resource(resourceKey, null);
        }

        public NodeBuilder resource(boolean primary, Consumer<NodeBuilder> consumer) {
            return resource(null, primary, consumer);
        }

        public NodeBuilder resource(Consumer<NodeBuilder> consumer) {
            return resource(null, consumer);
        }

        public NodeBuilder resource(String resourceKey, Consumer<NodeBuilder> consumer) {
            return resource(resourceKey, false, consumer);
        }

        public NodeBuilder resource(String resourceKey, boolean primary, Consumer<NodeBuilder> consumer) {
            var resource = getResourceBuilder(resourceKey);
            if (null != consumer) consumer.accept(resource);

            return resource(resource.node, primary);
        }

        public NodeBuilder resource(String resourceKey, boolean primary) {
            return resource(resourceKey, primary, null);
        }

        public NodeBuilder resource(Node resource) {
            return this.child(resource);
        }

        public NodeBuilder resource(Node resource, boolean primary) {
            entityManager.persist(
                    NodeConnection.create(node, resource, Relevance.CORE, NodeConnectionType.BRANCH, primary));

            contextUpdaterService.updateContexts(resource);

            return this;
        }

        public NodeBuilder translation(String languageCode, Consumer<JsonTranslationBuilder> consumer) {
            var nodeTranslation = node.addTranslation("", languageCode);
            var builder = new JsonTranslationBuilder(nodeTranslation);
            consumer.accept(builder);
            entityManager.persist(node);
            return this;
        }

        public NodeBuilder translation(String name, String languageCode) {
            node.addTranslation(name, languageCode);
            entityManager.persist(node);
            return this;
        }
    }
}
