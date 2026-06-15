/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceTypeTranslationsTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types?language=nb");
        ResourceTypeDTO[] resourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);
        assertAnyTrue(resourceTypes, s -> "Fagstoff".equals(s.getName()));
    }

    @Test
    public void can_get_single_resource_type() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, "nb");
        assertEquals("Fagstoff", resourceType.getName());
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, "XX");
        assertEquals("Fagstoff", resourceType.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, null);
        assertEquals("Fagstoff", resourceType.getName());
    }

    @Test
    public void can_get_all_translations() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        TranslationDTO[] translations = testUtils.getObject(
                TranslationDTO[].class, testUtils.getResource("/v1/resource-types/" + id + "/translations"));
        assertEquals(4, translations.length);
        assertAnyTrue(translations, t -> "Fagstoff".equals(t.name) && "nb".equals(t.language));
        assertAnyTrue(translations, t -> "Subject Material".equals(t.name) && "en".equals(t.language));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        TranslationDTO translation = testUtils.getObject(
                TranslationDTO.class, testUtils.getResource("/v1/resource-types/" + id + "/translations/nb"));
        assertEquals("Fagstoff", translation.name);
        assertEquals("nb", translation.language);
    }

    private ResourceTypeDTO getResourceTypeIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/resource-types/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(ResourceTypeDTO.class, testUtils.getResource(path));
    }
}
