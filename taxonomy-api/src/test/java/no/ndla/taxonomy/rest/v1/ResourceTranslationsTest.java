/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceTranslationsTest extends RestTest {

    @Test
    public void can_get_all_resources_with_translation() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.name("The inner planets")
                .translation("nb", tr -> tr.name("De indre planetene")));
        builder.node(NodeType.RESOURCE, r -> r.name("Gas giants").translation("nb", tr -> tr.name("Gasskjemper")));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources?language=nb");
        final var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "De indre planetene".equals(s.getName()));
        assertAnyTrue(resources, s -> "Gasskjemper".equals(s.getName()));
    }

    @Test
    public void can_get_single_resource_with_translation() throws Exception {
        URI trigonometry = builder.node(NodeType.RESOURCE, s -> s.name("introduction to trigonometry")
                        .translation("nb", tr -> tr.name("Introduksjon til trigonometri")))
                .getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/" + trigonometry + "?language=nb");
        final var resource = testUtils.getObject(NodeDTO.class, response);

        assertEquals("Introduksjon til trigonometri", resource.getName());
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra"))
                .getPublicId();
        final var resource = getResourceIndexDocument(id, "XX");
        assertEquals("Introduction to algrebra", resource.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra")
                        .translation("nb", l -> l.name("Introduksjon til algebra")))
                .getPublicId();

        final var resource = getResourceIndexDocument(id, null);
        assertEquals("Introduksjon til algebra", resource.getName());
    }

    @Test
    public void can_add_translation() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra"));
        URI id = resource.getPublicId();

        testUtils.updateResource("/v1/resources/" + id + "/translations/nb", new TranslationPUT() {
            {
                name = "Introduksjon til algebra";
            }
        });

        assertEquals(
                "Introduksjon til algebra",
                resource.getTranslation("nb").orElseThrow().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra")
                .translation("nb", l -> l.name("Introduksjon til algebra")));
        URI id = resource.getPublicId();

        testUtils.deleteResource("/v1/resources/" + id + "/translations/nb");

        assertNull(resource.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra")
                .translation("nb", l -> l.name("Introduksjon til algebra"))
                .translation("en", l -> l.name("Introduction to algrebra"))
                .translation("de", l -> l.name("Introduktion bis Algebra")));
        URI id = resource.getPublicId();

        TranslationDTO[] translations = testUtils.getObject(
                TranslationDTO[].class, testUtils.getResource("/v1/resources/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> "Introduksjon til algebra".equals(t.name) && "nb".equals(t.language));
        assertAnyTrue(translations, t -> "Introduction to algrebra".equals(t.name) && "en".equals(t.language));
        assertAnyTrue(translations, t -> "Introduktion bis Algebra".equals(t.name) && "de".equals(t.language));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, t -> t.name("Introduction to algrebra")
                .translation("nb", l -> l.name("Introduksjon til algebra")));
        URI id = resource.getPublicId();

        TranslationDTO translation = testUtils.getObject(
                TranslationDTO.class, testUtils.getResource("/v1/resources/" + id + "/translations/nb"));
        assertEquals("Introduksjon til algebra", translation.name);
        assertEquals("nb", translation.language);
    }

    private NodeDTO getResourceIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/resources/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(NodeDTO.class, testUtils.getResource(path));
    }
}
