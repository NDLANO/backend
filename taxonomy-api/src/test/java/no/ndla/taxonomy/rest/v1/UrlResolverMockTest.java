/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.rest.v1.dtos.ResolvedOldUrl;
import no.ndla.taxonomy.rest.v1.dtos.UrlMapping;
import no.ndla.taxonomy.service.AbstractIntegrationTest;
import no.ndla.taxonomy.service.UrlResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
/*
 * Test controller only
 */
public class UrlResolverMockTest extends AbstractIntegrationTest {
    @MockitoBean
    UrlResolverService urlResolverService;

    private MockMvc mvc;

    @Autowired
    private TestUtils testUtils;

    @BeforeEach
    public void setUp(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void resolveOldUrl404WhenNotImported() throws Exception {
        String oldUrl = "no/such/path";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(Optional.empty());
        ResultActions result = mvc.perform(get("/v1/url/mapping?url=" + oldUrl).accept(APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void resolveOldUrlExpectNewPathWhenImported() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        String newPath = "subject:11/topic:1:183926";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(Optional.of(newPath));
        ResultActions result = mvc.perform(get("/v1/url/mapping?url=" + oldUrl).accept(APPLICATION_JSON));

        result.andExpect(status().isOk());
        ResolvedOldUrl resolvedOldUrl =
                testUtils.getObject(ResolvedOldUrl.class, result.andReturn().getResponse());
        assertEquals(newPath, resolvedOldUrl.path);
    }

    @Test
    public void putOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");

        ResultActions result = mvc.perform(put("/v1/url/mapping")
                .content(new ObjectMapper().writeValueAsString(new UrlMapping(oldUrl, nodeId, subjectId)))
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNoContent());
        verify(this.urlResolverService, times(1)).putUrlMapping(oldUrl, nodeId, subjectId);
    }

    @Test
    public void putOldUrlBadParameters() throws Exception {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.url = "ndla.no/nb/node/183926?fag=127013";
        urlMapping.nodeId = "b a d";
        urlMapping.subjectId = "b a d";

        ResultActions result = mvc.perform(put("/v1/url/mapping")
                .content(new ObjectMapper().writeValueAsString(urlMapping))
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
    }

    @Test
    public void putBadOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");

        doThrow(new UrlResolverService.NodeIdNotFoundExeption(""))
                .when(this.urlResolverService)
                .putUrlMapping(any(), any(), any());

        ResultActions result = mvc.perform(put("/v1/url/mapping")
                .content(new ObjectMapper().writeValueAsString(new UrlMapping(oldUrl, nodeId, subjectId)))
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }
}
