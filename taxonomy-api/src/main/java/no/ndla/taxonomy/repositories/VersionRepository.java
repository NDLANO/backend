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
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import org.springframework.data.jpa.repository.Query;

public interface VersionRepository extends TaxonomyRepository<Version> {
    @Query(value = "SELECT * from Version v where v.public_id = :#{#publicId.toString()}", nativeQuery = true)
    Optional<Version> findFirstByPublicId(URI publicId);

    @Query(value = "SELECT * from Version v where v.hash = :hash", nativeQuery = true)
    Optional<Version> findFirstByHash(String hash);

    @Query(value = "SELECT * from Version v where v.version_type = :#{#versionType.name()}", nativeQuery = true)
    List<Version> findByVersionType(VersionType versionType);

    @Query(value = "SELECT * from Version v where v.version_type = :#{#versionType.name()}", nativeQuery = true)
    Optional<Version> findFirstByVersionType(VersionType versionType);
}
