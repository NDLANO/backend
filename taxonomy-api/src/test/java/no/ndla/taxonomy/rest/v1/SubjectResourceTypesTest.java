/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import org.junit.jupiter.api.Test;

public class SubjectResourceTypesTest extends RestTest {

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, s -> s.isContext(true).child(NodeType.TOPIC, t -> t.name("topic")
                        .child(
                                NodeType.TOPIC,
                                st -> st.resource(r -> r.name("resource 1")
                                        .resourceType(rt -> rt.name("lecure"))
                                        .resourceType(rt -> rt.name("assignment"))))
                        .resource(r -> r.name("resource 2")
                                .resourceType(rt -> rt.name("lecture"))
                                .resourceType(rt -> rt.name("assignment")))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].getResourceTypes().size());
        assertEquals(2, result[1].getResourceTypes().size());
    }

    @Test
    public void can_have_no_resource_type() throws Exception {
        URI id = builder.node(
                        NodeType.SUBJECT,
                        n -> n.isContext(true).name("subject").child(NodeType.TOPIC, t -> t.name("topic")
                                .child(NodeType.TOPIC, st -> st.resource(r -> r.name("resource 1")))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].getResourceTypes().size());
    }

    @Test
    public void can_get_resources_for_a_subject_filtered_on_resource_type() throws Exception {
        builder.resourceType("assignment");
        URI lecture = builder.resourceType("lecture").getPublicId();

        URI id = builder.node(NodeType.SUBJECT, n -> n.isContext(true).child(t -> t.name("a")
                        .child(NodeType.TOPIC, sub -> sub.name("subtopic")
                                .resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                        .resource(r -> r.name("an assignment").resourceType("assignment"))
                        .resource(r -> r.name("a lecture").resourceType("lecture"))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources?type=" + lecture);
        var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.getName()));
    }
}
