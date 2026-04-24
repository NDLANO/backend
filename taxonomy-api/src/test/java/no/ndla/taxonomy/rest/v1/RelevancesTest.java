/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RelevancesTest extends RestTest {

    @Test
    public void can_get_a_single_relevance() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/relevances/" + "urn:relevance:core");
        RelevanceDTO relevance = testUtils.getObject(RelevanceDTO.class, response);

        assertEquals("Kjernestoff", relevance.name);
    }

    @Test
    public void can_get_all_relevances() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/relevances");
        RelevanceDTO[] relevances = testUtils.getObject(RelevanceDTO[].class, response);

        assertEquals(2, relevances.length);
        assertAnyTrue(relevances, f -> "Kjernestoff".equals(f.name));
        assertAnyTrue(relevances, f -> "Tilleggsstoff".equals(f.name));
    }

    @Test
    public void get_unknown_relevance_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/relevances/nonexistantid", status().isNotFound());
    }
}
