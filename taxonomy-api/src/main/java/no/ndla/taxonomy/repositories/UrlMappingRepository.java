/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.util.List;
import no.ndla.taxonomy.domain.UrlMapping;
import org.springframework.data.repository.CrudRepository;

public interface UrlMappingRepository extends CrudRepository<UrlMapping, String> {
    List<UrlMapping> findAllByOldUrlLike(String oldUrl);

    List<UrlMapping> findAllByOldUrl(String oldUrl);
}
