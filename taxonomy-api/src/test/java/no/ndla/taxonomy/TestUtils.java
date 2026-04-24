/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.WebApplicationContext;

@Component
public class TestUtils {

    private final HttpMessageConverter mappingJackson2HttpMessageConverter;
    private final EntityManager entityManager;
    private final MockMvc mockMvc;

    @Autowired
    public TestUtils(
            HttpMessageConverter<?>[] converters,
            WebApplicationContext webApplicationContext,
            EntityManager entityManager) {
        mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        this.entityManager = entityManager;

        assertNotNull("the JSON message converter must not be null", mappingJackson2HttpMessageConverter);
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    public String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    public MockHttpServletResponse createResource(String path, Object command) throws Exception {
        return createResource(path, command, status().isCreated());
    }

    public MockHttpServletResponse createResource(String path) throws Exception {
        entityManager.flush();
        return mockMvc.perform(post(path)).andReturn().getResponse();
    }

    public MockHttpServletResponse createResource(String path, Object command, ResultMatcher resultMatcher)
            throws Exception {
        entityManager.flush();
        return mockMvc.perform(post(path).contentType(APPLICATION_JSON).content(json(command)))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    public MockHttpServletResponse getResource(String path, ResultMatcher resultMatcher) throws Exception {
        entityManager.flush();
        return mockMvc.perform(get(path).accept(APPLICATION_JSON))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    public MockHttpServletResponse getResource(String path) throws Exception {
        return getResource(path, status().isOk());
    }

    public MockHttpServletResponse deleteResource(String path) throws Exception {
        return deleteResource(path, status().isNoContent());
    }

    public MockHttpServletResponse deleteResource(String path, ResultMatcher resultMatcher) throws Exception {
        entityManager.flush();
        return mockMvc.perform(delete(path))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    public MockHttpServletResponse updateResource(String path) throws Exception {
        return updateResource(path, null, status().isNoContent());
    }

    public MockHttpServletResponse updateResource(String path, Object command) throws Exception {
        return updateResource(path, command, status().isNoContent());
    }

    public MockHttpServletResponse updateResourceRawInput(String path, String rawInput) throws Exception {
        return updateResourceRawInput(path, rawInput, status().isNoContent());
    }

    public MockHttpServletResponse updateResourceRawInput(String path, String rawInput, ResultMatcher resultMatcher)
            throws Exception {
        return mockMvc.perform(put(path).contentType(APPLICATION_JSON).content(rawInput))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    public MockHttpServletResponse updateResource(String path, Object command, ResultMatcher resultMatcher)
            throws Exception {
        entityManager.flush();
        if (command == null)
            return mockMvc.perform(put(path).contentType(APPLICATION_JSON))
                    .andExpect(resultMatcher)
                    .andReturn()
                    .getResponse();
        else
            return mockMvc.perform(put(path).contentType(APPLICATION_JSON).content(json(command)))
                    .andExpect(resultMatcher)
                    .andReturn()
                    .getResponse();
    }

    public static URI getId(MockHttpServletResponse response) {
        String location = response.getHeader("Location");
        return URI.create(location.substring(location.lastIndexOf("/") + 1));
    }

    public <V> V getObject(Class<V> theClass, MockHttpServletResponse response) throws Exception {
        MockHttpInputMessage mockHttpInputMessage = new MockHttpInputMessage(response.getContentAsByteArray());
        return (V) mappingJackson2HttpMessageConverter.read(theClass, mockHttpInputMessage);
    }

    public static <V> void assertAnyTrue(V[] objects, Predicate<V> predicate) {
        assertTrue(objects.length > 0, "Array was empty");
        String className = objects[0].getClass().getSimpleName();
        assertTrue(Arrays.stream(objects).anyMatch(predicate), "No " + className + " matching predicate found.");
    }

    public static <V> void assertAnyTrue(Iterable<V> objects, Predicate<V> predicate) {
        assertAnyTrue(objects.iterator(), predicate);
    }

    public static <V> void assertAnyTrue(Iterator<V> objects, Predicate<V> predicate) {
        assertTrue(objects.hasNext());

        String className = null;
        while (objects.hasNext()) {
            V next = objects.next();
            className = next.getClass().getSimpleName();
            if (predicate.test(next)) return;
        }

        if (null == className) fail("Empty collection");
        fail("No " + className + " matching predicate found.");
    }

    public static <V> void assertAllTrue(V[] objects, Predicate<V> predicate) {
        assertTrue(Arrays.stream(objects).allMatch(predicate));
    }

    public static <T> T first(Iterable<T> iterable) {
        return iterable.iterator().next();
    }

    public static boolean isValidId(URI id) {
        return id != null && id.toString().contains("urn");
    }
}
