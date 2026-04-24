/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import java.util.Optional;
import java.util.concurrent.Callable;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.service.VersionContext;

public abstract class Task<T extends DomainEntity> implements Callable<T> {
    private String version;

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public final T call() throws Exception {
        VersionContext.setCurrentVersion(this.version);
        return execute().orElse(null);
    }

    protected abstract Optional<T> execute();
}
