/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TreeSorterTest {
    @Test
    public void sortList() throws URISyntaxException {
        final var sorter = new TreeSorter();

        final var l1a = new TestSortable(null, new URI("urn:l1a"), 1);
        final var l1b = new TestSortable(null, new URI("urn:l1b"), 2);
        final var l1c = new TestSortable(null, new URI("urn:l1c"), 3);
        final var l1d = new TestSortable(null, new URI("urn:l1d"), 4);

        final var l1a2a = new TestSortable(new URI("urn:l1a"), new URI("urn:l1a2a"), 1);
        final var l1a2b = new TestSortable(new URI("urn:l1a"), new URI("urn:l1a2b"), 2);
        final var l1c2a = new TestSortable(new URI("urn:l1c"), new URI("urn:l1c2a"), 1);
        final var l1c2b = new TestSortable(new URI("urn:l1c"), new URI("urn:l1c2b"), 2);

        final var l1c2b3a = new TestSortable(new URI("urn:l1c2b"), new URI("urn:l1c2b3a"), 1);
        final var l1c2b3b = new TestSortable(new URI("urn:l1c2b"), new URI("urn:l1c2b3b"), 2);

        // Adding in any order to a collection (sorted or not) and sending for sorting must return a
        // sorted list
        final var sortedList =
                sorter.sortList(Set.of(l1c2b3b, l1a2b, l1c, l1c2b, l1c2b3a, l1a, l1b, l1c2a, l1d, l1a2a));

        assertEquals(10, sortedList.size());
        assertEquals("urn:l1a", sortedList.get(0).getSortableId().toString());
        assertEquals("urn:l1a2a", sortedList.get(1).getSortableId().toString());
        assertEquals("urn:l1a2b", sortedList.get(2).getSortableId().toString());
        assertEquals("urn:l1b", sortedList.get(3).getSortableId().toString());
        assertEquals("urn:l1c", sortedList.get(4).getSortableId().toString());
        assertEquals("urn:l1c2a", sortedList.get(5).getSortableId().toString());
        assertEquals("urn:l1c2b", sortedList.get(6).getSortableId().toString());
        assertEquals("urn:l1c2b3a", sortedList.get(7).getSortableId().toString());
        assertEquals("urn:l1c2b3b", sortedList.get(8).getSortableId().toString());
        assertEquals("urn:l1d", sortedList.get(9).getSortableId().toString());
    }

    private record TestSortable(URI parentId, URI id, int rank) implements TreeSorter.Sortable {

        @Override
        public int getSortableRank() {
            return rank;
        }

        @Override
        public URI getSortableId() {
            return id;
        }

        @Override
        public URI getSortableParentId() {
            return parentId;
        }
    }
}
