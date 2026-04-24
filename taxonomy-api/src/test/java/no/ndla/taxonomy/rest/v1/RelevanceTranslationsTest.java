/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RelevanceTranslationsTest extends RestTest {

    @Test
    public void can_get_all_relevances() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/relevances?language=nb");
        RelevanceDTO[] relevances = testUtils.getObject(RelevanceDTO[].class, response);

        assertEquals(2, relevances.length);
        assertAnyTrue(relevances, s -> "Kjernestoff".equals(s.name));
        assertAnyTrue(relevances, s -> "Tilleggsstoff".equals(s.name));
    }

    @Test
    public void can_get_single_relevance() throws Exception {
        RelevanceDTO relevanceDTO = getRelevanceDTO(URI.create("urn:relevance:core"), "nb");
        assertEquals("Kjernestoff", relevanceDTO.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        RelevanceDTO relevance = getRelevanceDTO(URI.create("urn:relevance:core"), "XX");
        assertEquals("Kjernestoff", relevance.name);
    }

    @Test
    public void can_get_all_translations() throws Exception {
        URI id = URI.create("urn:relevance:core");
        TranslationDTO[] translations = testUtils.getObject(
                TranslationDTO[].class, testUtils.getResource("/v1/relevances/" + id + "/translations"));

        assertEquals(4, translations.length);
        assertAnyTrue(translations, t -> "Kjernestoff".equals(t.name) && "nb".equals(t.language));
        assertAnyTrue(translations, t -> "Core content".equals(t.name) && "en".equals(t.language));
        assertAnyTrue(translations, t -> "Kjernestoff".equals(t.name) && "nn".equals(t.language));
        assertAnyTrue(translations, t -> "Guovddášávnnas".equals(t.name) && "se".equals(t.language));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        URI id = URI.create("urn:relevance:core");

        TranslationDTO translation = testUtils.getObject(
                TranslationDTO.class, testUtils.getResource("/v1/relevances/" + id + "/translations/nb"));
        assertEquals("Kjernestoff", translation.name);
        assertEquals("nb", translation.language);
    }

    private RelevanceDTO getRelevanceDTO(URI id, String language) throws Exception {
        String path = "/v1/relevances/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(RelevanceDTO.class, testUtils.getResource(path));
    }
}
