/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DomainEntityTest {
    private DomainEntity domainEntity;

    @BeforeEach
    public void setUp() {
        domainEntity = spy(DomainEntity.class);
    }

    @Test
    public void getId() {
        setField(domainEntity, "id", 1000);
        assertEquals(Integer.valueOf(1000), domainEntity.getId());
    }

    @Test
    public void getAndSetPublicId() throws URISyntaxException {
        assertNull(domainEntity.getPublicId());
        domainEntity.setPublicId(new URI("urn:test1"));
        assertEquals(new URI("urn:test1"), domainEntity.getPublicId());
    }
}
