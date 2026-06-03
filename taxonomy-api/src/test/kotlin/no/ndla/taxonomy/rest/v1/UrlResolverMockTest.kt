/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import no.ndla.taxonomy.TestUtils
import no.ndla.taxonomy.rest.v1.dtos.ResolvedOldUrl
import no.ndla.taxonomy.rest.v1.dtos.UrlMapping
import no.ndla.taxonomy.service.AbstractIntegrationTest
import no.ndla.taxonomy.service.UrlResolverService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureMockMvc
/*
 * Test controller only
 */
class UrlResolverMockTest : AbstractIntegrationTest() {

  @MockitoBean private lateinit var urlResolverService: UrlResolverService

  private lateinit var mvc: MockMvc

  @Autowired private lateinit var testUtils: TestUtils

  @BeforeEach
  fun setUp(context: WebApplicationContext) {
    mvc = MockMvcBuilders.webAppContextSetup(context).build()
  }

  @Test
  fun resolveOldUrl404WhenNotImported() {
    val oldUrl = "no/such/path"

    given(urlResolverService.resolveOldUrl(oldUrl)).willReturn(null)
    val result = mvc.perform(get("/v1/url/mapping?url=$oldUrl").accept(APPLICATION_JSON))

    result.andExpect(status().isNotFound())
  }

  @Test
  fun resolveOldUrlExpectNewPathWhenImported() {
    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    val newPath = "subject:11/topic:1:183926"

    given(urlResolverService.resolveOldUrl(oldUrl)).willReturn(newPath)
    val result = mvc.perform(get("/v1/url/mapping?url=$oldUrl").accept(APPLICATION_JSON))

    result.andExpect(status().isOk())
    val resolvedOldUrl =
        testUtils.getObject(ResolvedOldUrl::class.java, result.andReturn().response)
    assertEquals(newPath, resolvedOldUrl.path)
  }

  @Test
  fun putOldUrl() {
    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    val nodeId = URI.create("urn:topic:1:183926")
    val subjectId = URI.create("urn:subject:11")

    val result =
        mvc.perform(
            put("/v1/url/mapping")
                .content(ObjectMapper().writeValueAsString(UrlMapping(oldUrl, nodeId, subjectId)))
                .contentType(APPLICATION_JSON))

    result.andExpect(status().isNoContent())
    verify(urlResolverService, times(1)).putUrlMapping(oldUrl, nodeId, subjectId)
  }

  @Test
  fun putOldUrlBadParameters() {
    val mapping = UrlMapping("ndla.no/nb/node/183926?fag=127013", "b a d", "b a d")

    val result =
        mvc.perform(
            put("/v1/url/mapping")
                .content(ObjectMapper().writeValueAsString(mapping))
                .contentType(APPLICATION_JSON))

    result.andExpect(status().isBadRequest())
  }

  @Test
  fun putBadOldUrl() {
    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    val nodeId = URI.create("urn:topic:1:183926")
    val subjectId = URI.create("urn:subject:11")

    doThrow(UrlResolverService.NodeIdNotFoundException(""))
        .`when`(urlResolverService)
        .putUrlMapping(oldUrl, nodeId, subjectId)

    val result =
        mvc.perform(
            put("/v1/url/mapping")
                .content(ObjectMapper().writeValueAsString(UrlMapping(oldUrl, nodeId, subjectId)))
                .contentType(APPLICATION_JSON))

    result.andExpect(status().isNotFound())
  }
}
