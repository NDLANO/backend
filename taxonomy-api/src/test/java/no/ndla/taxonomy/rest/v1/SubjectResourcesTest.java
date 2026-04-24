/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SubjectResourcesTest extends RestTest {
    @Autowired
    private TestSeeder testSeeder;

    @Test
    public void can_get_resources_belonging_to_a_resource_type_for_a_subject() throws Exception {
        testSeeder.resourceWithResourceTypeTestSetup();

        var response = testUtils.getResource("/v1/subjects/urn:subject:1/resources?type=urn:resourcetype:video");
        var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(1, result.length);
        assertAnyTrue(result, r -> "R:1".equals(r.getName()));
    }
}
