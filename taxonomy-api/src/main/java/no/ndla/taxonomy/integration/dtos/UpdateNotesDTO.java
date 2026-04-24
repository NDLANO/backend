/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collection;

public record UpdateNotesDTO(@JsonProperty Collection<DraftNotesDTO> data) implements Serializable {}
