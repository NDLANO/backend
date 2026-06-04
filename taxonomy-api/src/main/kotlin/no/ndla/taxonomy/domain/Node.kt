package no.ndla.taxonomy.domain

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException
import no.ndla.taxonomy.util.PrettyUrlUtil
import org.hibernate.annotations.Type
import org.slf4j.LoggerFactory

@Entity
class Node() : DomainObject(), EntityWithMetadata {

    companion object {
        private val logger = LoggerFactory.getLogger(Node::class.java)
    }

    @OneToMany(mappedBy = "child", cascade = [CascadeType.ALL], orphanRemoval = true)
    var parentConnections: MutableSet<NodeConnection> = TreeSet()
        protected set

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    var childConnections: MutableSet<NodeConnection> = TreeSet()
        protected set

    @Column
    var contentUri: URI? = null

    @Column
    @Enumerated(EnumType.STRING)
    var nodeType: NodeType? = null
        set(value) {
            field = value
            updatePublicID()
        }

    @Column
    var ident: String? = null
        set(value) {
            field = value
            updatePublicID()
        }

    @Column
    var created_at: Instant? = null

    @Column
    var updated_at: Instant? = null

    @Column
    private var context: Boolean = false

    fun isContext(): Boolean {
        return this.context
    }

    fun setContext(context: Boolean) {
        this.context = context
    }

    @OneToMany(mappedBy = "node", cascade = [CascadeType.ALL], orphanRemoval = true)
    var resourceResourceTypes: MutableSet<ResourceResourceType> = TreeSet()

    @Column(name = "visible")
    private var visible: Boolean = true

    @Type(JsonBinaryType::class)
    @Column(name = "translations", columnDefinition = "jsonb")
    override var translations: MutableList<JsonTranslation> = ArrayList()

    @Type(JsonBinaryType::class)
    @Column(name = "grepcodes", columnDefinition = "jsonb")
    var grepcodes: MutableSet<JsonGrepCode> = HashSet()

    @Type(JsonBinaryType::class)
    @Column(name = "customfields", columnDefinition = "jsonb")
    var customfields: MutableMap<String, String> = HashMap()

    @Type(JsonBinaryType::class)
    @Column(name = "contexts", columnDefinition = "jsonb")
    var contexts: MutableSet<TaxonomyContext> = HashSet()

    @Type(JsonBinaryType::class)
    @Column(name = "contextids", columnDefinition = "jsonb")
    var contextIds: MutableSet<String> = HashSet()

    @Column(name = "quality_evaluation")
    @Convert(converter = GradeConverter::class)
    var qualityEvaluation: Grade? = null

    @Column(name = "quality_evaluation_comment")
    private var qualityEvaluationComment: String? = null

    @Column(name = "child_quality_evaluation_sum")
    var childQualityEvaluationSum: Int = 0

    @Column(name = "child_quality_evaluation_count")
    var childQualityEvaluationCount: Int = 0

    @Column(name = "requires_technical_evaluation")
    private var requiresTechnicalEvaluation: Boolean? = null

    @Column(name = "technical_evaluation_comment")
    private var technicalEvaluationComment: String? = null

    constructor(nodeType: NodeType) : this() {
        this.nodeType = nodeType
        this.ident = UUID.randomUUID().toString()
        updatePublicID()
    }

    constructor(node: Node, keepPublicId: Boolean = true) : this() {
        this.contentUri = node.contentUri
        this.nodeType = node.nodeType
        this.ident = node.ident
        this.context = node.isContext()
        this.qualityEvaluation = node.qualityEvaluationGrade.orElse(null)
        this.qualityEvaluationComment = node.getQualityEvaluationComment().orElse(null)

        if (keepPublicId) {
            this.publicId = node.publicId
        } else {
            this.ident = UUID.randomUUID().toString()
            updatePublicID()
        }

        this.translations = node.translations.map { JsonTranslation(it) }.toMutableList()
        val rrts = TreeSet<ResourceResourceType>()
        for (rt in node.resourceResourceTypes) {
            val rrt = ResourceResourceType()
            if (keepPublicId) {
                rrt.publicId = rt.publicId
            }
            rrt.node = this
            rrt.resourceType = rt.resourceType
            rrts.add(rrt)
        }
        this.resourceResourceTypes = rrts
        this.setMetadata(Metadata(node.metadata))
        this.name = node.name
    }

    @PrePersist
    fun prePersist() {
        val now = Instant.now()
        setCreatedAt(now)
        setUpdatedAt(now)
    }

    @PreUpdate
    fun preUpdate() {
        setUpdatedAt(Instant.now())
    }

    val qualityEvaluationGrade: Optional<Grade>
        get() = Optional.ofNullable(qualityEvaluation)

    fun getQualityEvaluationComment(): Optional<String> {
        return Optional.ofNullable(qualityEvaluationComment)
    }

    fun getQualityEvaluationNote(): Optional<String> {
        return Optional.ofNullable(qualityEvaluationComment)
    }

    val childQualityEvaluationAverage: Optional<GradeAverage>
        get() {
            if (this.childQualityEvaluationCount == 0 || this.childQualityEvaluationSum == 0) {
                return Optional.empty()
            }
            val gradeAverage = GradeAverage(this.childQualityEvaluationSum, this.childQualityEvaluationCount)
            return Optional.of(gradeAverage)
        }

    fun setChildQualityEvaluationAverage(averageSum: Int, count: Int) {
        if (count <= 0 || averageSum <= 0) {
            this.childQualityEvaluationSum = 0
            this.childQualityEvaluationCount = 0
            return
        }

        this.childQualityEvaluationSum = averageSum
        this.childQualityEvaluationCount = count
    }

    fun addGradeAverageTreeToAverageCalculation(newGradeAverage: GradeAverage) {
        val childAvg = childQualityEvaluationAverage
        if (childAvg.isEmpty) {
            this.childQualityEvaluationSum = newGradeAverage.averageSum
            this.childQualityEvaluationCount = newGradeAverage.count
            return
        }

        val oldSum = childAvg.get().averageSum
        val sumToAdd = newGradeAverage.averageSum
        val newSum = oldSum + sumToAdd
        val newCount = childAvg.get().count + newGradeAverage.count

        if (newCount > 0) {
            this.childQualityEvaluationSum = newSum
            this.childQualityEvaluationCount = newCount
        } else {
            this.childQualityEvaluationSum = 0
            this.childQualityEvaluationCount = 0
        }
    }

    fun removeGradeAverageTreeFromAverageCalculation(previousGradeAverage: GradeAverage) {
        val childAvg = childQualityEvaluationAverage
        if (childAvg.isEmpty) {
            logger.error(
                "Tried to remove {} from node '{}' but child average is missing. This seems like a bug or data inconsistency.",
                previousGradeAverage,
                this.publicId
            )
            return
        }

        val totalSum = childAvg.get().averageSum
        val sumToRemove = previousGradeAverage.averageSum

        val newSum = totalSum - sumToRemove
        val newCount = childAvg.get().count - previousGradeAverage.count

        if (newCount <= 0 || newSum <= 0) {
            this.childQualityEvaluationSum = 0
            this.childQualityEvaluationCount = 0
        } else {
            this.childQualityEvaluationSum = newSum
            this.childQualityEvaluationCount = newCount
        }
    }

    fun updateChildQualityEvaluationAverage(previousGrade: Optional<Grade>, newGrade: Optional<Grade>) {
        val childAvg = childQualityEvaluationAverage
        if (childAvg.isEmpty) {
            newGrade.ifPresent { ng ->
                this.childQualityEvaluationSum = ng.toInt()
                this.childQualityEvaluationCount = 1
            }
            return
        }

        val avg = childAvg.get()
        if (previousGrade.isEmpty && newGrade.isEmpty) return
        else if (previousGrade.isEmpty) { // New grade is present
            val newCount = avg.count + 1
            val newSum = avg.averageSum + newGrade.get().toInt()
            this.childQualityEvaluationCount = newCount
            this.childQualityEvaluationSum = newSum
        } else if (newGrade.isEmpty) { // Previous grade is present
            val newCount = avg.count - 1
            val oldSum = avg.averageSum
            val newSum = oldSum - previousGrade.get().toInt()
            if (newCount <= 0 || newSum <= 0) {
                this.childQualityEvaluationCount = 0
                this.childQualityEvaluationSum = 0
            } else {
                this.childQualityEvaluationCount = newCount
                this.childQualityEvaluationSum = newSum
            }
        } else { // Both grades are present
            val oldSum = avg.averageSum
            val newSum = oldSum - previousGrade.get().toInt() + newGrade.get().toInt()
            this.childQualityEvaluationCount = avg.count
            this.childQualityEvaluationSum = newSum
        }
    }

    fun updateEntireAverageTree() {
        val allChildGrades = childGradesRecursively
        val gradeAverage = GradeAverage.fromGrades(allChildGrades)
        logger.info(
            "Found average grades for {} children of node '{}' -> {}",
            allChildGrades.size,
            this.publicId,
            gradeAverage
        )

        if (gradeAverage.count == 0) {
            this.childQualityEvaluationSum = 0
            this.childQualityEvaluationCount = 0
        } else if (gradeAverage.count > 0) {
            this.childQualityEvaluationSum = gradeAverage.averageSum
            this.childQualityEvaluationCount = gradeAverage.count
        }
    }

    val childGradesRecursively: List<Optional<Grade>>
        get() {
            val children = childNodesForQualityEvaluation
            return children.flatMap { child ->
                val childGrades = ArrayList(child.childGradesRecursively)
                if (child.nodeType == NodeType.RESOURCE) {
                    childGrades.add(child.qualityEvaluationGrade)
                }
                childGrades
            }
        }



    fun setQualityEvaluationComment(qualityEvaluationComment: Optional<String>) {
        this.qualityEvaluationComment = qualityEvaluationComment.orElse(null)
    }

    fun requiresTechnicalEvaluation(): Optional<Boolean> {
        return Optional.ofNullable(requiresTechnicalEvaluation)
    }

    fun setRequiresTechnicalEvaluation(requiresTechnicalEvaluation: Optional<Boolean>) {
        this.requiresTechnicalEvaluation = requiresTechnicalEvaluation.orElse(null)
    }

    fun getTechnicalEvaluationComment(): Optional<String> {
        return Optional.ofNullable(technicalEvaluationComment)
    }

    fun setTechnicalEvaluationComment(technicalEvaluationComment: Optional<String>) {
        this.technicalEvaluationComment = technicalEvaluationComment.orElse(null)
    }

    val pathPart: String
        get() = "/" + publicId!!.schemeSpecificPart

    private fun updatePublicID() {
        if (nodeType != null && ident != null) {
            super.setPublicId(URI.create("urn:" + nodeType!!.getName() + ":" + ident))
        }
    }

    val primaryPath: Optional<String>
        get() = pickContext(Optional.empty(), Optional.empty(), Optional.empty(), NodeConnectionType.BRANCH, emptySet())
            .map { it.path() }

    fun pickContext(
        contextId: Optional<String>,
        parent: Optional<Node>,
        root: Optional<Node>,
        connectionType: NodeConnectionType,
        contextSet: Set<TaxonomyContext>
    ): Optional<TaxonomyContext> {
        val ctxs = if (contextSet.isNotEmpty()) contextSet else contexts
        val maybeContext = contextId.flatMap { id ->
            ctxs.stream().filter { c -> c.contextId() == id }.findFirst()
        }
        if (maybeContext.isPresent) {
            return maybeContext
        }
        val containsParent = parent.map { p ->
            ctxs.filter { c -> c.parentIds().contains(p.publicId.toString()) }.toSet()
        }.orElse(ctxs)
        val containsRoot = root.map { p ->
            containsParent.filter { c -> c.parentIds().contains(p.publicId.toString()) }.toSet()
        }.orElse(containsParent)

        val filtered = if (connectionType == NodeConnectionType.LINK || containsRoot.isEmpty()) contextSet else containsRoot
        return filtered.stream().min { context1, context2 ->
            val inPath1 = context1.path().contains(root.map { it.pathPart }.orElse("other"))
            val inPath2 = context2.path().contains(root.map { it.pathPart }.orElse("other"))

            if (inPath1 && inPath2) {
                if (context1.isPrimary && context2.isPrimary) {
                    return@min context1.parentIds().size - context2.parentIds().size
                }
                if (context1.isPrimary) return@min -1
                if (context2.isPrimary) return@min 1
            }
            if (inPath1 && !inPath2) return@min -1
            if (inPath2 && !inPath1) return@min 1
            if (context1.isPrimary && !context2.isPrimary) return@min -1
            if (context2.isPrimary && !context1.isPrimary) return@min 1

            context1.parentIds().size - context2.parentIds().size
        }
    }

    val allPaths: TreeSet<String>
        get() = contexts.map { it.path() }.toCollection(TreeSet())



    val resourceChildren: Collection<NodeConnection>
        get() = childConnections.filter { cc ->
            cc.child.map { child -> child.nodeType == NodeType.RESOURCE }.orElse(false)
        }.toSet()

    val resourceTypes: Collection<ResourceType>
        get() = resourceResourceTypes.map { it.resourceType }.toCollection(TreeSet())

    fun addResourceType(resourceType: ResourceType): ResourceResourceType {
        if (resourceTypes.contains(resourceType)) {
            throw DuplicateIdException("Resource with id ${publicId} is already marked with resource type with id ${resourceType.publicId}")
        }

        val resourceResourceType = ResourceResourceType.create(this, resourceType)
        addResourceResourceType(resourceResourceType)
        return resourceResourceType
    }

    fun removeResourceType(resourceType: ResourceType) {
        val resourceResourceType = getResourceType(resourceType)
        if (resourceResourceType.isEmpty)
            throw ChildNotFoundException("Resource with id ${this.publicId} is not of type ${resourceType.publicId}")

        this.resourceResourceTypes.remove(resourceResourceType.get())
    }

    private fun getResourceType(resourceType: ResourceType): Optional<ResourceResourceType> {
        for (resourceResourceType in resourceResourceTypes) {
            if (resourceResourceType.resourceType == resourceType) return Optional.of(resourceResourceType)
        }
        return Optional.empty()
    }

    fun addResourceResourceType(resourceResourceType: ResourceResourceType) {
        if (this.nodeType != NodeType.RESOURCE)
            throw IllegalArgumentException("ResourceResourceType can only be associated with ${NodeType.RESOURCE}")

        this.resourceResourceTypes.add(resourceResourceType)

        if (resourceResourceType.node != this) {
            throw IllegalArgumentException("ResourceResourceType must have Resource set before being associated with Resource")
        }
    }

    fun removeResourceResourceType(resourceResourceType: ResourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType)

        if (resourceResourceType.node == this) {
            resourceResourceType.disassociate()
        }
    }

    fun addChildConnection(nodeConnection: NodeConnection) {
        if (nodeConnection.parent.orElse(null) != this) {
            throw IllegalArgumentException("Parent must be set on NodeConnection before associating with child")
        }
        if (this.nodeType == NodeType.RESOURCE) {
            throw IllegalArgumentException("'${NodeType.RESOURCE}' nodes cannot have children")
        }

        this.childConnections.add(nodeConnection)
    }

    fun removeChildConnection(nodeConnection: NodeConnection) {
        this.childConnections.remove(nodeConnection)
        nodeConnection.disassociate()
    }

    fun addParentConnection(nodeConnection: NodeConnection) {
        if (nodeConnection.child.orElse(null) != this) {
            throw IllegalArgumentException("Child must be set on NodeConnection before associating with Parent")
        }

        this.parentConnections.add(nodeConnection)
    }

    fun removeParentConnection(nodeConnection: NodeConnection) {
        this.parentConnections.remove(nodeConnection)
        nodeConnection.disassociate()
    }

    val childNodes: Collection<Node>
        get() = childConnections.map { it.child }.filter { it.isPresent }.map { it.get() }

    val childNodesForQualityEvaluation: Collection<Node>
        get() = childConnections.filter { it.connectionType != NodeConnectionType.LINK }
            .map { it.child }
            .filter { it.isPresent }
            .map { it.get() }

    val parentNodes: Collection<Node>
        get() = parentConnections.map { it.parent }.filter { it.isPresent }.map { it.get() }

    val parentNodesForQualityEvaluation: Collection<Node>
        get() = parentConnections.filter { it.connectionType != NodeConnectionType.LINK }
            .map { it.parent }
            .filter { it.isPresent }
            .map { it.get() }

    val resources: Collection<Node>
        get() = childConnections.map { it.child }
            .filter { it.isPresent }
            .map { it.get() }
            .filter { it.nodeType == NodeType.RESOURCE }

    private fun getAllParentsRecursive(): Collection<Node> {
        val parents = parentNodes
        val all = ArrayList(parents)
        parents.forEach { parent -> all.addAll(parent.getAllParentsRecursive()) }
        return all
    }

    val allParentContexts: Set<TaxonomyContext>
        get() = getAllParentsRecursive().flatMap { it.contexts }.toSet()

    val contextType: Optional<String>
        get() {
            if (contentUri == null) return Optional.empty()
            if (contentUri!!.schemeSpecificPart.startsWith("learningpath")) return Optional.of("learningpath")
            if (NodeType.TOPIC == nodeType) return Optional.of("topic-article")
            if (contentUri!!.schemeSpecificPart.startsWith("article")) return Optional.of("standard")
            return Optional.empty()
        }

    fun addContextIds(contextId: Set<String>) {
        this.contextIds.addAll(contextId)
    }

    override fun getEntityName(): String {
        return nodeType?.getName() ?: "node"
    }

    override fun setPublicId(publicId: URI) {
        val idParts = publicId.toString().split(":")
        ident = java.lang.String.join(":", *idParts.toTypedArray().copyOfRange(2, idParts.size))
    }

    fun name(name: String): Node {
        this.name = name
        return this
    }

    override val metadata: Metadata
        get() = Metadata(this)

    override fun getGrepCodes(): Set<JsonGrepCode> {
        return this.grepcodes
    }

    override fun setCustomField(key: String, value: String) {
        this.customfields[key] = value
    }

    override fun unsetCustomField(key: String) {
        this.customfields.remove(key)
    }

    override fun setGrepCodes(codes: Set<JsonGrepCode>) {
        this.grepcodes = codes.toMutableSet()
    }

    override fun setCustomFields(customFields: Map<String, String>) {
        this.customfields = customFields.toMutableMap()
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    override fun setUpdatedAt(updatedAt: Instant?) {
        this.updated_at = updatedAt
    }

    override fun setCreatedAt(createdAt: Instant?) {
        this.created_at = createdAt
    }

    override fun isVisible(): Boolean {
        return this.visible
    }

    override fun getCreatedAt(): Instant? {
        return this.created_at
    }

    override fun getUpdatedAt(): Instant? {
        return this.updated_at
    }

    override fun getCustomFields(): Map<String, String> {
        return this.customfields
    }

    @PreRemove
    fun preRemove() {
        java.util.Set.copyOf(childConnections).forEach { it.disassociate() }
        java.util.Set.copyOf(parentConnections).forEach { it.disassociate() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Node
        return context == that.context &&
                contentUri == that.contentUri &&
                nodeType == that.nodeType &&
                ident == that.ident &&
                translations == that.translations &&
                customfields == that.customfields &&
                visible == that.visible &&
                grepcodes == that.grepcodes
    }

    override fun hashCode(): Int {
        return Objects.hash(context, contentUri, nodeType, ident, translations, customfields, visible, grepcodes)
    }

    val primaryNode: Optional<Node>
        get() {
            for (node in this.parentConnections) {
                if (node.isPrimary.orElse(false)) return node.parent
            }
            return Optional.empty()
        }

    fun addGrepCode(code: String) {
        val now = Instant.now().toString()
        val newGrepCode = JsonGrepCode(code, now, now)
        this.grepcodes.add(newGrepCode)
    }



    fun translatedPrettyNames(): Set<String> {
        val pretties = this.translations.mapNotNull { it.name }
            .map { PrettyUrlUtil.prettyName(it) }
            .toMutableSet()
        prettyName.ifPresent { pretties.add(it) }
        return pretties
    }

    val prettyName: Optional<String>
        get() {
            val defaultTranslation = this.getTranslatedName(Constants.DefaultLanguage)
            val name = Optional.ofNullable(defaultTranslation)
            return name.map { PrettyUrlUtil.prettyName(it) }
        }
}
