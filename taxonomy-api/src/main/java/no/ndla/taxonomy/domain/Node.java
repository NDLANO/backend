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
import java.util.stream.Collectors;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.util.PrettyUrlUtil;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Node extends DomainObject implements EntityWithMetadata {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Node.class);

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> parentConnections = new TreeSet<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> childConnections = new TreeSet<>();

    @Column
    private URI contentUri;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @Column
    private String ident;

    @Column
    @CreationTimestamp
    private Instant created_at;

    @Column
    @UpdateTimestamp
    private Instant updated_at;

    @Column
    private boolean context;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new TreeSet<>();

    @Column
    private boolean visible = true;

    @Type(JsonBinaryType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    private List<JsonTranslation> translations = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(name = "grepcodes", columnDefinition = "jsonb")
    private Set<JsonGrepCode> grepcodes = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(name = "customfields", columnDefinition = "jsonb")
    private Map<String, String> customfields = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(name = "contexts", columnDefinition = "jsonb")
    private Set<TaxonomyContext> contexts = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(name = "contextids", columnDefinition = "jsonb")
    private Set<String> contextIds = new HashSet<>();

    @Column(name = "quality_evaluation")
    @Convert(converter = GradeConverter.class)
    private Grade qualityEvaluation;

    @Column(name = "quality_evaluation_comment")
    private String qualityEvaluationComment;

    @Column(name = "child_quality_evaluation_sum")
    private int childQualityEvaluationSum;

    @Column(name = "child_quality_evaluation_count")
    private int childQualityEvaluationCount;

    @Column(name = "requires_technical_evaluation")
    private Boolean requiresTechnicalEvaluation;

    @Column(name = "technical_evaluation_comment")
    private String technicalEvaluationComment;

    // Needed for hibernate
    public Node() {}

    public Node(NodeType nodeType) {
        setNodeType(nodeType);
        setIdent(UUID.randomUUID().toString());
        updatePublicID();
    }

    public Node(Node node) {
        this(node, true);
    }

    public Node(Node node, boolean keepPublicId) {
        this.contentUri = node.getContentUri();
        this.nodeType = node.getNodeType();
        this.ident = node.getIdent();
        this.context = node.isContext();
        this.qualityEvaluation = node.getQualityEvaluationGrade().orElse(null);
        this.qualityEvaluationComment = node.getQualityEvaluationComment().orElse(null);

        if (keepPublicId) {
            setPublicId(node.getPublicId());
        } else {
            setIdent(UUID.randomUUID().toString());
            updatePublicID();
        }

        this.translations =
                node.getTranslations().stream().map(JsonTranslation::new).toList();
        TreeSet<ResourceResourceType> rrts = new TreeSet<>();
        for (ResourceResourceType rt : node.getResourceResourceTypes()) {
            ResourceResourceType rrt = new ResourceResourceType();
            if (keepPublicId) {
                rrt.setPublicId(rt.getPublicId());
            }
            rrt.setNode(this);
            rrt.setResourceType(rt.getResourceType());
            rrts.add(rrt);
        }
        this.resourceResourceTypes = rrts;
        setMetadata(new Metadata(node.getMetadata()));
        setName(node.getName());
    }

    public Optional<Grade> getQualityEvaluationGrade() {
        return Optional.ofNullable(qualityEvaluation);
    }

    public Optional<String> getQualityEvaluationComment() {
        return Optional.ofNullable(qualityEvaluationComment);
    }

    public Optional<String> getQualityEvaluationNote() {
        return Optional.ofNullable(qualityEvaluationComment);
    }

    public Optional<GradeAverage> getChildQualityEvaluationAverage() {
        if (this.childQualityEvaluationCount == 0 || this.childQualityEvaluationSum == 0) {
            return Optional.empty();
        }
        var gradeAverage = new GradeAverage(this.childQualityEvaluationSum, this.childQualityEvaluationCount);
        return Optional.of(gradeAverage);
    }

    public void setChildQualityEvaluationAverage(int averageSum, int count) {
        if (count <= 0 || averageSum <= 0) {
            this.childQualityEvaluationSum = 0;
            this.childQualityEvaluationCount = 0;
            return;
        }

        this.childQualityEvaluationSum = averageSum;
        this.childQualityEvaluationCount = count;
    }

    public void addGradeAverageTreeToAverageCalculation(GradeAverage newGradeAverage) {
        var childAvg = getChildQualityEvaluationAverage();
        if (childAvg.isEmpty()) {
            this.childQualityEvaluationSum = newGradeAverage.getAverageSum();
            this.childQualityEvaluationCount = newGradeAverage.getCount();
            return;
        }

        var oldSum = childAvg.get().getAverageSum();
        var sumToAdd = newGradeAverage.getAverageSum();
        var newSum = oldSum + sumToAdd;
        var newCount = childAvg.get().getCount() + newGradeAverage.getCount();

        if (newCount > 0) {
            this.childQualityEvaluationSum = newSum;
            this.childQualityEvaluationCount = newCount;
        } else {
            this.childQualityEvaluationSum = 0;
            this.childQualityEvaluationCount = 0;
        }
    }

    public void removeGradeAverageTreeFromAverageCalculation(GradeAverage previousGradeAverage) {
        var childAvg = getChildQualityEvaluationAverage();
        if (childAvg.isEmpty()) {
            logger.error(
                    "Tried to remove {} from node '{}' but child average is missing. This seems like a bug or data inconsistency.",
                    previousGradeAverage,
                    this.getPublicId());
            return;
        }

        var totalSum = childAvg.get().getAverageSum();
        var sumToRemove = previousGradeAverage.getAverageSum();

        var newSum = totalSum - sumToRemove;
        var newCount = childAvg.get().getCount() - previousGradeAverage.getCount();

        if (newCount <= 0 || newSum <= 0) {
            this.childQualityEvaluationSum = 0;
            this.childQualityEvaluationCount = 0;
        } else {
            this.childQualityEvaluationSum = newSum;
            this.childQualityEvaluationCount = newCount;
        }
    }

    public void updateChildQualityEvaluationAverage(Optional<Grade> previousGrade, Optional<Grade> newGrade) {
        var childAvg = getChildQualityEvaluationAverage();
        if (childAvg.isEmpty()) {
            newGrade.ifPresent(ng -> {
                this.childQualityEvaluationSum = ng.toInt();
                this.childQualityEvaluationCount = 1;
            });
            return;
        }

        var avg = childAvg.get();
        if (previousGrade.isEmpty() && newGrade.isEmpty()) return;
        else if (previousGrade.isEmpty()) { // New grade is present
            var newCount = avg.getCount() + 1;
            var newSum = avg.averageSum + newGrade.get().toInt();
            this.childQualityEvaluationCount = newCount;
            this.childQualityEvaluationSum = newSum;
        } else if (newGrade.isEmpty()) { // Previous grade is present
            var newCount = avg.getCount() - 1;
            var oldSum = avg.getAverageSum();
            var newSum = oldSum - previousGrade.get().toInt();
            if (newCount <= 0 || newSum <= 0) {
                this.childQualityEvaluationCount = 0;
                this.childQualityEvaluationSum = 0;
            } else {
                this.childQualityEvaluationCount = newCount;
                this.childQualityEvaluationSum = newSum;
            }
        } else { // Both grades are present
            var oldSum = avg.getAverageSum();
            var newSum = oldSum - previousGrade.get().toInt() + newGrade.get().toInt();
            this.childQualityEvaluationCount = avg.getCount();
            this.childQualityEvaluationSum = newSum;
        }
    }

    public void updateEntireAverageTree() {
        var allChildGrades = getChildGradesRecursively();
        var gradeAverage = GradeAverage.fromGrades(allChildGrades);
        logger.info(
                "Found average grades for {} children of node '{}' -> {}",
                allChildGrades.size(),
                this.getPublicId(),
                gradeAverage);

        if (gradeAverage.count == 0) {
            this.childQualityEvaluationSum = 0;
            this.childQualityEvaluationCount = 0;
        } else if (gradeAverage.count > 0) {
            this.childQualityEvaluationSum = gradeAverage.getAverageSum();
            this.childQualityEvaluationCount = gradeAverage.count;
        }
    }

    public List<Optional<Grade>> getChildGradesRecursively() {
        var children = getChildNodesForQualityEvaluation();
        return children.stream()
                .flatMap(child -> {
                    ArrayList<Optional<Grade>> childGrades = new ArrayList<>(child.getChildGradesRecursively());
                    if (child.nodeType == NodeType.RESOURCE) {
                        childGrades.add(child.getQualityEvaluationGrade());
                    }
                    return childGrades.stream();
                })
                .toList();
    }

    public void setQualityEvaluation(Grade qualityEvaluation) {
        this.qualityEvaluation = qualityEvaluation;
    }

    public void setQualityEvaluationComment(Optional<String> qualityEvaluationComment) {
        this.qualityEvaluationComment = qualityEvaluationComment.orElse(null);
    }

    public Optional<Boolean> requiresTechnicalEvaluation() {
        return Optional.ofNullable(requiresTechnicalEvaluation);
    }

    public void setRequiresTechnicalEvaluation(Optional<Boolean> requiresTechnicalEvaluation) {
        this.requiresTechnicalEvaluation = requiresTechnicalEvaluation.orElse(null);
    }

    public Optional<String> getTechnicalEvaluationComment() {
        return Optional.ofNullable(technicalEvaluationComment);
    }

    public void setTechnicalEvaluationComment(Optional<String> technicalEvaluationComment) {
        this.technicalEvaluationComment = technicalEvaluationComment.orElse(null);
    }

    public String getPathPart() {
        return "/" + getPublicId().getSchemeSpecificPart();
    }

    private void updatePublicID() {
        super.setPublicId(URI.create("urn:" + nodeType.getName() + ":" + getIdent()));
    }

    public Optional<String> getPrimaryPath() {
        return pickContext(Optional.empty(), Optional.empty(), Optional.empty(), NodeConnectionType.BRANCH, Set.of())
                .map(TaxonomyContext::path);
    }

    /**
     * Picks a context based on parameters. If no parameters or no matches, pick by comparing path and primary
     *
     * @param contextId      If this is present, return the context with corresponding id
     * @param parent         If this is present, filter contexts where parent is in parentIds
     * @param root           If this is present, return context with this publicId as root. Else pick context containing roots
     *                       publicId.
     * @param connectionType If this is present and of type LINK, return the default context
     * @param contextSet     Possibly filtered set of contexts to choose from
     * @return Context
     */
    public Optional<TaxonomyContext> pickContext(
            Optional<String> contextId,
            Optional<Node> parent,
            Optional<Node> root,
            NodeConnectionType connectionType,
            Set<TaxonomyContext> contextSet) {
        var contexts = !contextSet.isEmpty() ? contextSet : getContexts();
        var maybeContext = contextId.flatMap(
                id -> contexts.stream().filter(c -> c.contextId().equals(id)).findFirst());
        if (maybeContext.isPresent()) {
            return maybeContext;
        }
        var containsParent = parent.map(p -> contexts.stream()
                        .filter(c -> c.parentIds().contains(p.getPublicId().toString()))
                        .collect(Collectors.toSet()))
                .orElse(contexts);
        var containsRoot = root.map(p -> containsParent.stream()
                        .filter(c -> c.parentIds().contains(p.getPublicId().toString()))
                        .collect(Collectors.toSet()))
                .orElse(containsParent);
        // If no context is found based on params, or connection is of type LINK return the one with the shortest path
        var filtered = connectionType == NodeConnectionType.LINK || containsRoot.isEmpty() ? contextSet : containsRoot;
        return filtered.stream().min((context1, context2) -> {
            final var inPath1 =
                    context1.path().contains(root.map(Node::getPathPart).orElse("other"));
            final var inPath2 =
                    context2.path().contains(root.map(Node::getPathPart).orElse("other"));

            if (inPath1 && inPath2) {
                if (context1.isPrimary() && context2.isPrimary()) {
                    // contexts are of equal value, pick the shortest
                    return context1.parentIds().size() - context2.parentIds().size();
                }
                if (context1.isPrimary()) {
                    return -1;
                }
                if (context2.isPrimary()) {
                    return 1;
                }
            }
            if (inPath1 && !inPath2) {
                return -1;
            }
            if (inPath2 && !inPath1) {
                return 1;
            }
            if (context1.isPrimary() && !context2.isPrimary()) {
                return -1;
            }
            if (context2.isPrimary() && !context1.isPrimary()) {
                return 1;
            }
            // contexts are of equal value, pick the shortest
            return context1.parentIds().size() - context2.parentIds().size();
        });
    }

    public TreeSet<String> getAllPaths() {
        return getContexts().stream().map(TaxonomyContext::path).collect(Collectors.toCollection(TreeSet::new));
    }

    /*
     * In the old code the primary URL for topics was special since it would try to return a context URL (a topic
     * behaving as a subject) rather than a subject URL if that is available. Trying to re-implement it by sorting the
     * context URLs first by the same path comparing as the old code
     */
    /*
     * public Optional<String> getPrimaryPath() { return getCachedPaths() .stream()
     * .map(CachedPath::getPath).min((path1, path2) -> { if (path1.startsWith("/topic") && path2.startsWith("/topic")) {
     * return 0; }
     *
     * if (path1.startsWith("/topic")) { return -1; }
     *
     * if (path2.startsWith("/topic")) { return 1; }
     *
     * return 0; }); }
     */

    public Collection<NodeConnection> getParentConnections() {
        return this.parentConnections.stream().toList();
    }

    public Collection<NodeConnection> getChildConnections() {
        return new HashSet<>(childConnections);
    }

    public Collection<NodeConnection> getResourceChildren() {
        return this.childConnections.stream()
                .filter(cc -> cc.getChild()
                        .map(child -> child.getNodeType() == NodeType.RESOURCE)
                        .orElse(false))
                .collect(Collectors.toSet());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream()
                .map(ResourceResourceType::getResourceType)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Collection<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes;
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        if (getResourceTypes().contains(resourceType)) {
            throw new DuplicateIdException("Resource with id " + getPublicId()
                    + " is already marked with resource type with id " + resourceType.getPublicId());
        }

        ResourceResourceType resourceResourceType = ResourceResourceType.create(this, resourceType);
        addResourceResourceType(resourceResourceType);
        return resourceResourceType;
    }

    public void removeResourceType(ResourceType resourceType) {
        var resourceResourceType = getResourceType(resourceType);
        if (resourceResourceType.isEmpty())
            throw new ChildNotFoundException(
                    "Resource with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());

        this.resourceResourceTypes.remove(resourceResourceType.get());
    }

    private Optional<ResourceResourceType> getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType)) return Optional.of(resourceResourceType);
        }
        return Optional.empty();
    }

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        if (this.getNodeType() != NodeType.RESOURCE)
            throw new IllegalArgumentException("ResourceResourceType can only be associated with " + NodeType.RESOURCE);

        this.resourceResourceTypes.add(resourceResourceType);

        if (resourceResourceType.getNode() != this) {
            throw new IllegalArgumentException(
                    "ResourceResourceType must have Resource set before being associated with Resource");
        }
    }

    public void removeResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType);

        if (resourceResourceType.getNode() == this) {
            resourceResourceType.disassociate();
        }
    }

    public void addChildConnection(NodeConnection nodeConnection) {
        if (nodeConnection.getParent().orElse(null) != this) {
            throw new IllegalArgumentException("Parent must be set on NodeConnection before associating with child");
        }
        if (this.nodeType == NodeType.RESOURCE) {
            throw new IllegalArgumentException("'" + NodeType.RESOURCE + "' nodes cannot have children");
        }

        this.childConnections.add(nodeConnection);
    }

    public void removeChildConnection(NodeConnection nodeConnection) {
        this.childConnections.remove(nodeConnection);

        nodeConnection.disassociate();
    }

    public void addParentConnection(NodeConnection nodeConnection) {
        if (nodeConnection.getChild().orElse(null) != this) {
            throw new IllegalArgumentException("Child must be set on NodeConnection before associating with Parent");
        }

        this.parentConnections.add(nodeConnection);
    }

    public void removeParentConnection(NodeConnection nodeConnection) {
        this.parentConnections.remove(nodeConnection);

        nodeConnection.disassociate();
    }

    public Collection<Node> getChildNodes() {
        return childConnections.stream()
                .map(NodeConnection::getChild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getChildNodesForQualityEvaluation() {
        return childConnections.stream()
                .filter(connection -> connection.getConnectionType() != NodeConnectionType.LINK)
                .map(NodeConnection::getChild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getParentNodes() {
        return parentConnections.stream()
                .map(NodeConnection::getParent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getParentNodesForQualityEvaluation() {
        return parentConnections.stream()
                .filter(connection -> connection.getConnectionType() != NodeConnectionType.LINK)
                .map(NodeConnection::getParent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getResources() {
        return childConnections.stream()
                .map(NodeConnection::getChild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> s.getNodeType() == NodeType.RESOURCE)
                .toList();
    }

    private Collection<Node> getAllParentsRecursive() {
        var parents = getParentNodes();
        ArrayList<Node> all = new ArrayList<>(parents);
        parents.forEach(parent -> all.addAll(parent.getAllParentsRecursive()));
        return all;
    }

    public Set<TaxonomyContext> getAllParentContexts() {
        return getAllParentsRecursive().stream()
                .map(Node::getContexts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public void setIdent(String ident) {
        this.ident = ident;
        updatePublicID();
    }

    public String getIdent() {
        return ident;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public Optional<String> getContextType() {
        if (contentUri == null) return Optional.empty();
        if (contentUri.getSchemeSpecificPart().startsWith("learningpath")) return Optional.of("learningpath");
        if (NodeType.TOPIC.equals(nodeType)) return Optional.of("topic-article");
        if (contentUri.getSchemeSpecificPart().startsWith("article")) return Optional.of("standard");
        return Optional.empty();
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        updatePublicID();
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return context;
    }

    public Set<String> getContextIds() {
        return contextIds;
    }

    public void setContextIds(Set<String> contextIds) {
        this.contextIds = contextIds;
    }

    public void addContextIds(Set<String> contextId) {
        this.contextIds.addAll(contextId);
    }

    @Override
    public String getEntityName() {
        return nodeType.getName();
    }

    @Override
    public void setPublicId(URI publicId) {
        final String[] idParts = publicId.toString().split(":");
        setIdent(String.join(":", Arrays.copyOfRange(idParts, 2, idParts.length)));
    }

    public Node name(String name) {
        setName(name);
        return this;
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

    @PreRemove
    void preRemove() {
        Set.copyOf(childConnections).forEach(NodeConnection::disassociate);
        Set.copyOf(parentConnections).forEach(NodeConnection::disassociate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node that = (Node) o;
        return context == that.context
                && Objects.equals(contentUri, that.contentUri)
                && nodeType == that.nodeType
                && ident.equals(that.ident)
                && Objects.equals(translations, that.translations)
                && Objects.equals(this.customfields, that.customfields)
                && this.visible == that.visible
                && Objects.equals(this.grepcodes, that.grepcodes);
    }

    public Optional<Node> getPrimaryNode() {
        for (var node : this.parentConnections) {
            if (node.isPrimary().orElse(false)) return node.getParent();
        }
        return Optional.empty();
    }

    public Map<String, String> getCustomFields() {
        return this.customfields;
    }

    public void setContexts(Set<TaxonomyContext> contexts) {
        this.contexts = contexts;
    }

    public Set<TaxonomyContext> getContexts() {
        return this.contexts;
    }

    public void addGrepCode(String code) {
        var now = Instant.now().toString();
        var newGrepCode = new JsonGrepCode(code, now, now);
        this.grepcodes.add(newGrepCode);
    }

    @Override
    public List<JsonTranslation> getTranslations() {
        return this.translations;
    }

    @Override
    public void setTranslations(List<JsonTranslation> translations) {
        this.translations = translations;
    }

    public Set<String> translatedPrettyNames() {
        var pretties = this.translations.stream()
                .map(JsonTranslation::getName)
                .filter(Objects::nonNull)
                .map(PrettyUrlUtil::prettyName)
                .collect(Collectors.toSet());
        getPrettyName().ifPresent(pretties::add);
        return pretties;
    }

    public Optional<String> getPrettyName() {
        var defaultTranslation = this.getTranslatedName(Constants.DefaultLanguage);
        var name = Optional.ofNullable(defaultTranslation);
        return name.map(PrettyUrlUtil::prettyName);
    }
}
