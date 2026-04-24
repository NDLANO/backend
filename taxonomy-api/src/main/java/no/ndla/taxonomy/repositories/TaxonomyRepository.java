/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TaxonomyRepository<T> extends JpaRepository<T, Integer>, JpaSpecificationExecutor<T> {
    T findByPublicId(URI id);

    default T getByPublicId(URI id) {
        T entity = findByPublicId(id);
        if (null == entity) throw new NotFoundException("entity", id);
        return entity;
    }

    default void deleteAllAndFlush() {
        deleteAll();
        flush();
    }
}
