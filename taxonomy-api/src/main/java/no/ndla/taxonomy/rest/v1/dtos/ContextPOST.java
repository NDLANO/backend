/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

@Schema(requiredProperties = {"id"})
public class ContextPOST {
    public URI id;
}
