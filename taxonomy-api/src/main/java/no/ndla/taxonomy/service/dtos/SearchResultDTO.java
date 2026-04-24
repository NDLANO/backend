/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
        name = "SearchResult",
        requiredProperties = {"totalCount", "page", "pageSize", "results"})
public class SearchResultDTO<T> {
    @JsonProperty
    @Schema(example = "Total search result count, useful for fetching multiple pages")
    private long totalCount;

    @JsonProperty
    @Schema(example = "The page number")
    private int page;

    @JsonProperty
    @Schema(example = "The page size")
    private int pageSize;

    @JsonProperty
    @Schema(example = "List of search results")
    private List<T> results;

    public SearchResultDTO(long totalCount, int pageNumber, int pageSize, List<T> results) {
        this.totalCount = totalCount;
        this.page = pageNumber;
        this.pageSize = pageSize;
        this.results = results;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<T> getResults() {
        return results;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
