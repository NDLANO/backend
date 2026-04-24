/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class TreeSorter {
    private <T extends Sortable> List<T> addElements(Map<URI, Collection<T>> elementsToAddFrom, URI parentId) {
        final var itemsToAdd = new ArrayList<T>();

        elementsToAddFrom.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparingInt(Sortable::getSortableRank))
                .forEachOrdered(element -> {
                    itemsToAdd.add(element);
                    itemsToAdd.addAll(addElements(elementsToAddFrom, element.getSortableId()));
                });

        return itemsToAdd;
    }

    /**
     * Sorts all elements by tree structure and rank at each level
     *
     * <p>
     * 1 (rank 2) - 1:2-1 (rank 0) - 1:2:3-1 (rank 2) - 1:2:3-2 (rank 1) - 1:2-2 (rank 1) 2 (rank 1)
     *
     * <p>
     * Becomes a flat list of
     *
     * <p>
     * 2 1 1:2-1 1:2:3-2 1:2:3-1 1:2-2
     *
     * @param elements
     *            Elements to sort
     * @return sorted flat list
     */
    public <T extends Sortable> List<T> sortList(Collection<T> elements) {
        final var elementsByParent = new HashMap<URI, Collection<T>>();

        elements.forEach(element -> {
            var foundParent = false;
            if (element.getSortableParentId() != null) {
                for (var element2 : elements) {
                    if (element2.getSortableId().equals(element.getSortableParentId())) {
                        foundParent = true;
                    }
                }
            }

            final URI parentId;

            if (!foundParent) {
                parentId = null;
            } else {
                parentId = element.getSortableParentId();
            }

            elementsByParent.computeIfAbsent(parentId, k -> new ArrayList<>());
            elementsByParent.get(parentId).add(element);
        });

        return new ArrayList<>(this.addElements(elementsByParent, null));
    }

    public interface Sortable {
        @JsonIgnore
        int getSortableRank();

        @JsonIgnore
        URI getSortableId();

        @JsonIgnore
        URI getSortableParentId();
    }
}
