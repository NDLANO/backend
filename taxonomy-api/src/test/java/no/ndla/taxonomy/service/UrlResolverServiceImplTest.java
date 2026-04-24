/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.net.URI;
import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
/*
 * Test Service and database together.
 */
public class UrlResolverServiceImplTest extends AbstractIntegrationTest {
    @Autowired
    private Builder builder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private OldUrlCanonifier oldUrlCanonifier;

    private UrlResolverServiceImpl urlResolverService;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
    }

    @BeforeEach
    public void restTestSetUp() {
        urlResolverService = new UrlResolverServiceImpl(urlMappingRepository, nodeRepository, oldUrlCanonifier);
    }

    @Test
    @Transactional
    public void resolveOldUrl() {
        final String subjectId = "urn:subject:11";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        final String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping =
                builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void queryForNonExistingNodeShouldNotMatchSimilarNodeId() {
        String oldUrl = "ndla.no/node/54";
        String otherSubjectId = "urn:subject:1";
        String otherTopicId = "urn:topic:1:54321";
        String otherTopicUrl = "ndla.no/node/54321";

        // create another topic and mapping that should NOT match the query for the url above
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId(otherSubjectId)
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1:54321")));
        entityManager.persist(builder.urlMapping(
                c -> c.oldUrl(otherTopicUrl).public_id(otherTopicId).subject_id(otherSubjectId)));
        entityManager.flush();

        assertFalse(urlResolverService.resolveOldUrl(oldUrl).isPresent());
    }

    @Test
    @Transactional
    public void resolveOldUrlWithLanguage() {
        final String subjectId = "urn:subject:11";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        final String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping =
                builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService
                .resolveOldUrl("ndla.no/nb/node/183926?fag=127013")
                .orElseThrow();

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:2").child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedOrQueriedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:2").child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        String oldUrl = "ndla.no/node/183926";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenSubjectImportedButNotQueriedToPrimaryPath() {
        final String subjectId = "urn:subject:11";
        String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping =
                builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl("ndla.no/node/183926").orElseThrow();

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlBadSubjectPrimaryPath() {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1:183926")));
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(
                c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11"));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void putOldUrlForNonexistentResource() {
        assertThrows(
                Exception.class,
                () -> urlResolverService.putUrlMapping(
                        "abc", URI.create("urn:topic:1:12"), URI.create("urn:subject:12")));
    }

    @Test
    @Transactional
    public void putOldUrl() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void putOldUrlTwice() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void putOldUrlWithNoPaths() {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        try {
            urlResolverService.putUrlMapping(oldUrl, URI.create("urn:topic:1:283926"), URI.create(subjectId));
            fail("Expected NodeIdNotFoundExeption");
        } catch (UrlResolverService.NodeIdNotFoundExeption ignored) {

        }
    }

    @Test
    @Transactional
    public void putOldUrlWithSubjectQueryWithoutSubject() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId(subjectId).child(NodeType.TOPIC, t -> t.publicId(nodeId)));
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));

        entityManager.flush();

        String path = urlResolverService.resolveOldUrl("ndla.no/nb/node/183926").orElseThrow();
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveEntitiesFromPath() {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").name("Maths").child(NodeType.TOPIC, t -> t.publicId(
                                "urn:topic:1")
                        .name("Trignometry")
                        .resource("resource", resourceBuilder -> resourceBuilder
                                .publicId("urn:resource:1")
                                .name("Resource Name")
                                .contentUri(URI.create("urn:test:1")))));

        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:2").name("Biology").child(NodeType.TOPIC, t -> t.publicId(
                                "urn:topic:2")
                        .name("Mammals")
                        .resource("resource")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:3")
                .name("Chemistry")
                .child(NodeType.TOPIC, t -> {
                    t.publicId("urn:topic:3");
                    t.isContext(true);
                    t.name("Acids");
                    t.resource("resource");
                }));

        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:2").name("Resource Name"));

        // Four paths exists to the same resource:
        // /subject:1/topic:1/resource:1
        // /subject:2/topic:2/resource:2
        // /subject:3/topic:3/resource:2
        // /topic:3/resource:2

        {
            final var resolvedUrl = urlResolverService
                    .resolveUrl("/subject:1/topic:1/resource:1", "nb")
                    .orElseThrow();
            assertEquals(2, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/subject:1/topic:1/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/maths/resource-name/"));

            assertEquals("topic:1", parentIdList.get(0));
            assertEquals("subject:1", parentIdList.get(1));
        }

        {
            final var resolvedUrl = urlResolverService
                    .resolveUrl("/subject:2/topic:2/resource:1", "nb")
                    .orElseThrow();
            assertEquals(2, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/subject:2/topic:2/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/biology/resource-name/"));

            assertEquals("topic:2", parentIdList.get(0));
            assertEquals("subject:2", parentIdList.get(1));
        }

        {
            final var resolvedUrl =
                    urlResolverService.resolveUrl("/subject:2/topic:2", "nb").orElseThrow();
            assertEquals(1, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("topic:2", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("/subject:2/topic:2", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/e/biology/mammals/"));

            assertEquals("subject:2", parentIdList.getFirst());
        }
        {
            final var resolvedUrl =
                    urlResolverService.resolveUrl("/subject:2", "nb").orElseThrow();
            assertEquals(0, resolvedUrl.getParents().size());
            assertEquals("subject:2", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/f/biology/"));
        }

        {
            final var resolvedUrl =
                    urlResolverService.resolveUrl("/subject:2/", "nb").orElseThrow();
            assertEquals(0, resolvedUrl.getParents().size());
            assertEquals("subject:2", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/f/biology/"));
        }
        {
            final var resolvedUrl =
                    urlResolverService.resolveUrl("subject:2", "nb").orElseThrow();
            assertEquals(0, resolvedUrl.getParents().size());
            assertEquals("subject:2", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/f/biology/"));
        }

        // Test with a non-valid path to valid resource
        {
            var resolvedUrl = urlResolverService.resolveUrl("/subject:2/resource:1", "nb");
            assertTrue(resolvedUrl.isPresent());
            assertFalse(resolvedUrl.get().isExactMatch());
        }

        // Test with a non-valid path to valid resource
        {
            var resolvedUrl = urlResolverService.resolveUrl("/subject:2/topic:4/resource:1", "nb");
            assertTrue(resolvedUrl.isPresent());
            assertFalse(resolvedUrl.get().isExactMatch());
        }

        // Test with non-context node
        {
            assertFalse(
                    urlResolverService.resolveUrl("/topic:1/resource:2", "nb").isPresent());
            assertFalse(
                    urlResolverService.resolveUrl("/topic:2/resource:2", "nb").isPresent());
        }

        // Since topic3 is a context in itself, it would be valid to use it as root
        {
            final var resolvedUrl =
                    urlResolverService.resolveUrl("/topic:3/resource:1", "nb").orElseThrow();
            assertEquals(1, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/topic:3/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/acids/resource-name/"));

            assertEquals("topic:3", parentIdList.getFirst());
        }

        // Going via subject:3 is also valid
        {
            final var resolvedUrl = urlResolverService
                    .resolveUrl("/subject:3/topic:3/resource:1", "nb")
                    .orElseThrow();
            assertEquals(2, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/chemistry/resource-name/"));

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }

        // Additional slashes should make no difference
        {
            final var resolvedUrl = urlResolverService
                    .resolveUrl("////subject:3///topic:3//////resource:1///", "nb")
                    .orElseThrow();
            assertEquals(2, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/chemistry/resource-name/"));

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }

        // No leading slash should make no difference
        {
            final var resolvedUrl = urlResolverService
                    .resolveUrl("subject:3///topic:3//////resource:1///", "nb")
                    .orElseThrow();
            assertEquals(2, resolvedUrl.getParents().size());

            final var parentIdList = resolvedUrl.getParents().stream()
                    .map(URI::getSchemeSpecificPart)
                    .toList();

            assertEquals("resource:1", resolvedUrl.getId().getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.getName());
            assertEquals(URI.create("urn:test:1"), resolvedUrl.getContentUri());
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.getPath());
            assertTrue(resolvedUrl.getUrl().startsWith("/r/chemistry/resource-name/"));

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }
    }
}
