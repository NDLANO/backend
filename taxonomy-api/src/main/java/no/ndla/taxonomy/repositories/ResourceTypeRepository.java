/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.domain.ResourceType;
import org.springframework.data.jpa.repository.Query;

public interface ResourceTypeRepository extends TaxonomyRepository<ResourceType> {
    @Query("""
            SELECT DISTINCT rt
            FROM ResourceType rt
            LEFT JOIN FETCH rt.subtypes
            WHERE (:parent IS NULL AND rt.parent IS NULL) OR rt.parent = :parent
            ORDER BY rt.order ASC""")
    List<ResourceType> findAllByParentIncludingTranslationsAndFirstLevelSubtypes(ResourceType parent);

    @Query("SELECT DISTINCT rt FROM ResourceType rt WHERE rt.publicId = :publicId")
    Optional<ResourceType> findFirstByPublicIdIncludingTranslations(URI publicId);

    @Query(
            "SELECT DISTINCT rt FROM ResourceType rt JOIN rt.parent prt LEFT JOIN FETCH rt.subtypes WHERE prt.publicId = :publicId ORDER BY rt.order ASC")
    List<ResourceType> findAllByParentPublicIdIncludingTranslationsAndFirstLevelSubtypes(URI publicId);

    List<ResourceType> findAllByOrderByOrderAsc();

    @Query("SELECT COALESCE(MAX(rt.order), -1) + 1 FROM ResourceType rt")
    int nextOrderValue();
}
