export type paths = {
    "/search-api/v1/search/group": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Search across multiple groups of learning resources
         * @description Search across multiple groups of learning resources
         */
        get: operations["getSearch-apiV1SearchGroup"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/search-api/v1/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find learning resources
         * @description Shows all learning resources. You can search too.
         */
        get: operations["getSearch-apiV1Search"];
        put?: never;
        /**
         * Find learning resources
         * @description Shows all learning resources. You can search too.
         */
        post: operations["postSearch-apiV1Search"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/search-api/v1/search/editorial": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find draft learning resources
         * @description Shows all draft learning resources. You can search too.
         *     Query parameters are undocumented, but are the same as the body for the POST endpoint, except `kebab-case`.
         */
        get: operations["getSearch-apiV1SearchEditorial"];
        put?: never;
        /**
         * Find draft learning resources
         * @description Shows all draft learning resources. You can search too.
         */
        post: operations["postSearch-apiV1SearchEditorial"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/search-api/v1/search/subjects": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * List subjects with aggregated data about their contents
         * @description List subjects with aggregated data about their contents
         */
        post: operations["postSearch-apiV1SearchSubjects"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/search-api/v1/search/grep": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Search for grep codes
         * @description Search for grep codes
         */
        post: operations["postSearch-apiV1SearchGrep"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/search-api/v1/search/grep/replacements": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get grep replacements */
        get: operations["getSearch-apiV1SearchGrepReplacements"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
};
export type webhooks = Record<string, never>;
export type components = {
    schemas: {
        /** AllErrors */
        AllErrors: components["schemas"]["ErrorBody"] | components["schemas"]["NotFoundWithSupportedLanguages"] | components["schemas"]["ValidationErrorBody"];
        /** ApiTaxonomyContextDTO */
        ApiTaxonomyContextDTO: {
            /** @description Id of the taxonomy object. */
            publicId: string;
            /** @description Name of the root node this context is in. */
            root: string;
            /** @description Id of the root node this context is in. */
            rootId: string;
            /** @description The relevance for this context. */
            relevance: string;
            /** @description The relevanceId for this context. */
            relevanceId: string;
            /** @description Path to the resource in this context. */
            path: string;
            /** @description Breadcrumbs of path to the resource in this context. */
            breadcrumbs: string[];
            /** @description Unique id of this context. */
            contextId: string;
            /** @description Type in this context. */
            contextType: string;
            /** @description Resource-types of this context. */
            resourceTypes: components["schemas"]["TaxonomyResourceTypeDTO"][];
            /** @description Language for this context. */
            language: string;
            /** @description Whether this context is the primary connection */
            isPrimary: boolean;
            /** @description Whether this context is active */
            isActive: boolean;
            /** @description Unique url for this context. */
            url: string;
        };
        /**
         * ArticleTrait
         * @enum {string}
         */
        ArticleTrait: "VIDEO" | "AUDIO" | "PODCAST" | "INTERACTIVE";
        /**
         * CommentDTO
         * @description Information about a comment attached to an article
         */
        CommentDTO: {
            /** @description Id of the comment */
            id: string;
            /** @description Content of the comment */
            content: string;
            /** @description When the comment was created */
            created: string;
            /** @description When the comment was last updated */
            updated: string;
            /** @description If the comment is open or closed */
            isOpen: boolean;
            /** @description If the comment is solved or not */
            solved: boolean;
        };
        /**
         * DescriptionDTO
         * @description Title of resource
         */
        DescriptionDTO: {
            /** @description The freetext description of the resource */
            description: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
        };
        /** DraftSearchParamsDTO */
        DraftSearchParamsDTO: {
            /**
             * Format: int32
             * @description The page number of the search hits to display.
             */
            page?: number;
            /**
             * Format: int32
             * @description The number of search hits to display for each page.
             */
            pageSize?: number;
            /** @description A list of article-types the search should be filtered by. */
            articleTypes?: string[];
            /** @description A list of context-types the learning resources should be filtered by. */
            contextTypes?: string[];
            /** @description The ISO 639-1 language code describing language. */
            language?: string;
            /** @description Return only learning resources that have one of the provided ids. */
            ids?: number[];
            /** @description Return only learning resources of specific type(s). */
            resourceTypes?: string[];
            /** @description Return only results with provided license. */
            license?: string;
            /** @description Return only results with content matching the specified query. */
            query?: string;
            /** @description Return only results with notes matching the specified note-query. */
            noteQuery?: string;
            sort?: components["schemas"]["Sort"];
            /** @description Fallback to existing language if language is specified. */
            fallback?: boolean;
            /** @description A comma separated list of subjects the learning resources should be filtered by. */
            subjects?: string[];
            /** @description A list of ISO 639-1 language codes that the learning resource can be available in. */
            languageFilter?: string[];
            /**
             * @description A list of relevances the learning resources should be filtered by.
             *     If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
             *     If levels are specified the learning resource must have specified relevances in relation to a specified level.
             */
            relevance?: string[];
            /**
             * @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ["0", "initial", "start", "first"].
             *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
             *     This value may change between scrolls. Always use the one in the latest scroll result.
             */
            scrollId?: string;
            /** @description List of statuses to filter by. A draft only needs to have one of the available statuses to be included in result (OR). */
            draftStatus?: string[];
            /**
             * @description List of users to filter by.
             *     The value to search for is the user-id from Auth0.
             *     UpdatedBy on article and user in editorial-notes are searched.
             */
            users?: string[];
            /** @description A list of codes from GREP API the resources should be filtered by. */
            grepCodes?: string[];
            /** @description A comma separated list of traits the resources should be filtered by. */
            traits?: components["schemas"]["ArticleTrait"][];
            /** @description List of index-paths that should be term-aggregated and returned in result. */
            aggregatePaths?: string[];
            /** @description Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types. */
            embedResource?: string[];
            /** @description Return only results with embed data-resource_id, data-videoid or data-url with the specified id. */
            embedId?: string;
            /** @description Whether or not to include the 'other' status field when filtering with 'status' param. */
            includeOtherStatuses?: boolean;
            /** @description Return only results having next revision after this date. */
            revisionDateFrom?: string;
            /** @description Return only results having next revision before this date. */
            revisionDateTo?: string;
            /** @description Set to true to avoid including hits from the revision history log. */
            excludeRevisionLog?: boolean;
            /** @description List of responsible ids to filter by (OR filter). */
            responsibleIds?: string[];
            /** @description Filter out inactive taxonomy contexts. */
            filterInactive?: boolean;
            /** @description List of priority-levels to filter by. */
            priority?: components["schemas"]["Priority"][];
            /** @description A list of parent topics the learning resources should be filtered by. */
            topics?: string[];
            /** @description Return only results having published date after this date. */
            publishedDateFrom?: string;
            /** @description Return only results having published date before this date. */
            publishedDateTo?: string;
            /** @description Types of hits to appear in the result */
            resultTypes?: components["schemas"]["SearchType"][];
            /** @description Only return results that have one of the specified tags. */
            tags?: string[];
        };
        /**
         * ErrorBody
         * @description Information about an error
         */
        ErrorBody: {
            /** @description Code stating the type of error */
            code: string;
            /** @description Description of the error */
            description: string;
            /** @description When the error occurred */
            occurredAt: string;
            /**
             * Format: int32
             * @description Numeric http status code
             */
            statusCode: number;
        };
        /**
         * GrepKjerneelementDTO
         * @description Information about a single grep search result entry
         */
        GrepKjerneelementDTO: {
            /** @description The grep code */
            code: string;
            /** @description The grep status */
            status: components["schemas"]["GrepStatusDTO"];
            title: components["schemas"]["TitleDTO"];
            description: components["schemas"]["DescriptionDTO"];
            laereplan: components["schemas"]["GrepReferencedLaereplanDTO"];
            typename: components["schemas"]["GrepKjerneelementDTO1"];
        };
        /**
         * GrepKjerneelementDTO
         * @enum {string}
         */
        GrepKjerneelementDTO1: "GrepKjerneelementDTO";
        /**
         * GrepKompetansemaalDTO
         * @description Information about a single grep search result entry
         */
        GrepKompetansemaalDTO: {
            /** @description The grep code */
            code: string;
            /** @description The grep status */
            status: components["schemas"]["GrepStatusDTO"];
            title: components["schemas"]["TitleDTO"];
            laereplan: components["schemas"]["GrepReferencedLaereplanDTO"];
            kompetansemaalSett: components["schemas"]["GrepReferencedKompetansemaalSettDTO"];
            tverrfagligeTemaer: components["schemas"]["GrepTverrfagligTemaDTO"][];
            kjerneelementer: components["schemas"]["GrepReferencedKjerneelementDTO"][];
            reuseOf?: components["schemas"]["GrepReferencedKompetansemaalDTO"];
            typename: components["schemas"]["GrepKompetansemaalDTO1"];
        };
        /**
         * GrepKompetansemaalDTO
         * @enum {string}
         */
        GrepKompetansemaalDTO1: "GrepKompetansemaalDTO";
        /**
         * GrepKompetansemaalSettDTO
         * @description Information about a single grep search result entry
         */
        GrepKompetansemaalSettDTO: {
            /** @description The grep code */
            code: string;
            /** @description The grep status */
            status: components["schemas"]["GrepStatusDTO"];
            title: components["schemas"]["TitleDTO"];
            kompetansemaal: components["schemas"]["GrepReferencedKompetansemaalDTO"][];
            typename: components["schemas"]["GrepKompetansemaalSettDTO1"];
        };
        /**
         * GrepKompetansemaalSettDTO
         * @enum {string}
         */
        GrepKompetansemaalSettDTO1: "GrepKompetansemaalSettDTO";
        /**
         * GrepLaererplanDTO
         * @description Information about a single grep search result entry
         */
        GrepLaererplanDTO: {
            /** @description The grep code */
            code: string;
            /** @description The grep status */
            status: components["schemas"]["GrepStatusDTO"];
            title: components["schemas"]["TitleDTO"];
            replacedBy: components["schemas"]["GrepReferencedLaereplanDTO"][];
            typename: components["schemas"]["GrepLaererplanDTO1"];
        };
        /**
         * GrepLaererplanDTO
         * @enum {string}
         */
        GrepLaererplanDTO1: "GrepLaererplanDTO";
        /** GrepReferencedKjerneelementDTO */
        GrepReferencedKjerneelementDTO: {
            code: string;
            status: components["schemas"]["GrepStatusDTO"];
            title: string;
        };
        /** GrepReferencedKompetansemaalDTO */
        GrepReferencedKompetansemaalDTO: {
            code: string;
            status: components["schemas"]["GrepStatusDTO"];
            title: string;
        };
        /** GrepReferencedKompetansemaalSettDTO */
        GrepReferencedKompetansemaalSettDTO: {
            code: string;
            status: components["schemas"]["GrepStatusDTO"];
            title: string;
        };
        /** GrepReferencedLaereplanDTO */
        GrepReferencedLaereplanDTO: {
            code: string;
            status: components["schemas"]["GrepStatusDTO"];
            title: string;
        };
        /** GrepResultDTO */
        GrepResultDTO: components["schemas"]["GrepKjerneelementDTO"] | components["schemas"]["GrepKompetansemaalDTO"] | components["schemas"]["GrepKompetansemaalSettDTO"] | components["schemas"]["GrepLaererplanDTO"] | components["schemas"]["GrepTverrfagligTemaDTO"];
        /**
         * GrepSearchInputDTO
         * @description Input parameters to subject aggregations endpoint
         */
        GrepSearchInputDTO: {
            /** @description A comma separated list of prefixes that should be returned in the search. */
            prefixFilter?: string[];
            /** @description A comma separated list of codes that should be returned in the search. */
            codes?: string[];
            /** @description A query to filter the query by. */
            query?: string;
            /**
             * Format: int32
             * @description The page number of the search hits to display.
             */
            page?: number;
            /**
             * Format: int32
             * @description The number of search hits to display for each page.
             */
            pageSize?: number;
            sort?: components["schemas"]["GrepSortDTO"];
            /** @description The ISO 639-1 language code describing language used in query-params */
            language?: string;
        };
        /**
         * GrepSearchResultsDTO
         * @description Information about search-results
         */
        GrepSearchResultsDTO: {
            /**
             * Format: int64
             * @description The total number of resources matching this query
             */
            totalCount: number;
            /**
             * Format: int32
             * @description For which page results are shown from
             */
            page: number;
            /**
             * Format: int32
             * @description The number of results per page
             */
            pageSize: number;
            /** @description The chosen search language */
            language: string;
            /** @description The search results */
            results: components["schemas"]["GrepResultDTO"][];
        };
        /**
         * GrepSortDTO
         * @description The sort order of the search hits.
         * @enum {string}
         */
        GrepSortDTO: "-relevance" | "relevance" | "-title" | "title" | "-code" | "code" | "-status" | "status";
        /**
         * GrepStatusDTO
         * @enum {string}
         */
        GrepStatusDTO: "Published" | "InProgress" | "ToRevision" | "Expired" | "Invalid";
        /**
         * GrepTverrfagligTemaDTO
         * @description Information about a single grep search result entry
         */
        GrepTverrfagligTemaDTO: {
            /** @description The grep code */
            code: string;
            /** @description The grep status */
            status: components["schemas"]["GrepStatusDTO"];
            title: components["schemas"]["TitleDTO"];
            typename: components["schemas"]["GrepTverrfagligTemaDTO1"];
        };
        /**
         * GrepTverrfagligTemaDTO
         * @enum {string}
         */
        GrepTverrfagligTemaDTO1: "GrepTverrfagligTemaDTO";
        /**
         * GroupSearchResultDTO
         * @description Search result for group search
         */
        GroupSearchResultDTO: {
            /**
             * Format: int64
             * @description The total number of resources matching this query
             */
            totalCount: number;
            /**
             * Format: int32
             * @description For which page results are shown from
             */
            page?: number;
            /**
             * Format: int32
             * @description The number of results per page
             */
            pageSize: number;
            /** @description The chosen search language */
            language: string;
            /** @description The search results */
            results: components["schemas"]["MultiSummaryBaseDTO"][];
            /** @description The suggestions for other searches */
            suggestions: components["schemas"]["MultiSearchSuggestionDTO"][];
            /** @description The aggregated fields if specified in query */
            aggregations: components["schemas"]["MultiSearchTermsAggregationDTO"][];
            /** @description Type of resources in this object */
            resourceType: string;
        };
        /**
         * HighlightedFieldDTO
         * @description Object describing matched field with matching words emphasized
         */
        HighlightedFieldDTO: {
            /** @description Field that matched */
            field: string;
            /** @description List of segments that matched in `field` */
            matches: string[];
        };
        /**
         * LearningResourceType
         * @description Learning resource type
         * @enum {string}
         */
        LearningResourceType: "standard" | "topic-article" | "frontpage-article" | "learningpath" | "concept" | "gloss";
        /** Map_String */
        Map_String: {
            [key: string]: string;
        };
        /** MetaDescriptionDTO */
        MetaDescriptionDTO: {
            /** @description The meta description */
            metaDescription: string;
            /** @description The ISO 639-1 language code describing which article translation this meta description belongs to */
            language: string;
        };
        /**
         * MetaImageDTO
         * @description The meta image for the resource
         */
        MetaImageDTO: {
            /** @description The meta image id */
            url: string;
            /** @description The meta image alt text */
            alt: string;
            /** @description The ISO 639-1 language code describing which translation this meta image belongs to */
            language: string;
        };
        /**
         * MultiSearchResultDTO
         * @description Information about search-results
         */
        MultiSearchResultDTO: {
            /**
             * Format: int64
             * @description The total number of resources matching this query
             */
            totalCount: number;
            /**
             * Format: int32
             * @description For which page results are shown from
             */
            page?: number;
            /**
             * Format: int32
             * @description The number of results per page
             */
            pageSize: number;
            /** @description The chosen search language */
            language: string;
            /** @description The search results */
            results: components["schemas"]["MultiSummaryBaseDTO"][];
            /** @description The suggestions for other searches */
            suggestions: components["schemas"]["MultiSearchSuggestionDTO"][];
            /** @description The aggregated fields if specified in query */
            aggregations: components["schemas"]["MultiSearchTermsAggregationDTO"][];
        };
        /**
         * MultiSearchSuggestionDTO
         * @description Information about search-suggestions
         */
        MultiSearchSuggestionDTO: {
            /** @description The name of the field suggested for */
            name: string;
            /** @description The list of suggestions for given field */
            suggestions: components["schemas"]["SearchSuggestionDTO"][];
        };
        /**
         * MultiSearchSummaryDTO
         * @description Short summary of information about the resource
         */
        MultiSearchSummaryDTO: {
            /**
             * Format: int64
             * @description The unique id of the resource
             */
            id: number;
            title: components["schemas"]["TitleWithHtmlDTO"];
            /** @description The meta description of the resource */
            metaDescription: components["schemas"]["MetaDescriptionDTO"];
            metaImage?: components["schemas"]["MetaImageDTO"];
            /** @description Url pointing to the resource */
            url: string;
            /** @description Primary context of the resource */
            context?: components["schemas"]["ApiTaxonomyContextDTO"];
            /** @description Contexts of the resource */
            contexts: components["schemas"]["ApiTaxonomyContextDTO"][];
            /** @description Languages the resource exists in */
            supportedLanguages: string[];
            learningResourceType: components["schemas"]["LearningResourceType"];
            status?: components["schemas"]["StatusDTO"];
            /** @description Traits for the resource */
            traits: components["schemas"]["ArticleTrait"][];
            /**
             * Format: float
             * @description Relevance score. The higher the score, the better the document matches your search criteria.
             */
            score: number;
            /** @description List of objects describing matched field with matching words emphasized */
            highlights: components["schemas"]["HighlightedFieldDTO"][];
            /** @description The taxonomy paths for the resource */
            paths: string[];
            /** @description The time and date of last update */
            lastUpdated: string;
            /** @description Describes the license of the resource */
            license?: string;
            /** @description A list of revisions planned for the article */
            revisions: components["schemas"]["RevisionMetaDTO"][];
            responsible?: components["schemas"]["ResponsibleDTO"];
            /** @description Information about comments attached to the article */
            comments?: components["schemas"]["CommentDTO"][];
            /** @description If the article should be prioritized. Possible values are prioritized, on-hold, unspecified */
            priority?: components["schemas"]["Priority"];
            /** @description A combined resource type name if a standard article, otherwise the article type name */
            resourceTypeName?: string;
            /** @description Name of the parent topic if exists */
            parentTopicName?: string;
            /** @description Name of the primary context root if exists */
            primaryRootName?: string;
            /** @description When the article was last published */
            published?: string;
            /**
             * Format: int64
             * @description Number of times favorited in MyNDLA
             */
            favorited?: number;
            /** @description Type of the resource */
            resultType: components["schemas"]["SearchType"];
            typename: components["schemas"]["MultiSearchSummaryDTO1"];
        };
        /**
         * MultiSearchSummaryDTO
         * @enum {string}
         */
        MultiSearchSummaryDTO1: "MultiSearchSummaryDTO";
        /**
         * MultiSearchTermsAggregationDTO
         * @description Information about search aggregation on `field`
         */
        MultiSearchTermsAggregationDTO: {
            /** @description The field the specific aggregation is matching */
            field: string;
            /**
             * Format: int32
             * @description Number of documents with values that didn't appear in the aggregation. (Will only happen if there are more than 50 different values)
             */
            sumOtherDocCount: number;
            /**
             * Format: int32
             * @description The result is approximate, this gives an approximation of potential errors. (Specifics here: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-approximate-counts)
             */
            docCountErrorUpperBound: number;
            /** @description Values appearing in the field */
            values: components["schemas"]["TermValueDTO"][];
        };
        /** MultiSummaryBaseDTO */
        MultiSummaryBaseDTO: components["schemas"]["MultiSearchSummaryDTO"] | components["schemas"]["NodeHitDTO"];
        /** NodeHitDTO */
        NodeHitDTO: {
            /** @description The unique id of the taxonomy node */
            id: string;
            /** @description The title of the taxonomy node */
            title: string;
            /** @description The url to the frontend page of the taxonomy node */
            url?: string;
            subjectPage?: components["schemas"]["SubjectPageSummaryDTO"];
            /** @description Primary context of the resource */
            context?: components["schemas"]["ApiTaxonomyContextDTO"];
            /** @description Contexts of the resource */
            contexts: components["schemas"]["ApiTaxonomyContextDTO"][];
            typename: components["schemas"]["NodeHitDTO1"];
        };
        /**
         * NodeHitDTO
         * @enum {string}
         */
        NodeHitDTO1: "NodeHitDTO";
        /**
         * NodeType
         * @enum {string}
         */
        NodeType: "NODE" | "SUBJECT" | "TOPIC" | "RESOURCE" | "PROGRAMME";
        /**
         * NotFoundWithSupportedLanguages
         * @description Information about an error
         */
        NotFoundWithSupportedLanguages: {
            /** @description Code stating the type of error */
            code: string;
            /** @description Description of the error */
            description: string;
            /** @description When the error occurred */
            occurredAt: string;
            /** @description List of supported languages */
            supportedLanguages?: string[];
            /**
             * Format: int32
             * @description Numeric http status code
             */
            statusCode: number;
        };
        /**
         * Priority
         * @enum {string}
         */
        Priority: "prioritized" | "on-hold" | "unspecified";
        /**
         * ResponsibleDTO
         * @description Responsible field
         */
        ResponsibleDTO: {
            /** @description NDLA ID of responsible editor */
            responsibleId: string;
            /** @description Date of when the responsible editor was last updated */
            lastUpdated: string;
        };
        /**
         * RevisionMetaDTO
         * @description Information about the editorial notes
         */
        RevisionMetaDTO: {
            /** @description A date on which the article would need to be revised */
            revisionDate: string;
            /** @description Notes to keep track of what needs to happen before revision */
            note: string;
            /** @description Status of a revision, either 'revised' or 'needs-revision' */
            status: string;
        };
        /** SearchParamsDTO */
        SearchParamsDTO: {
            /**
             * Format: int32
             * @description The page number of the search hits to display.
             */
            page?: number;
            /**
             * Format: int32
             * @description The number of search hits to display for each page.
             */
            pageSize?: number;
            /** @description A list of article-types the search should be filtered by. */
            articleTypes?: string[];
            /**
             * @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ["0", "initial", "start", "first"].
             *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
             *     This value may change between scrolls. Always use the one in the latest scroll result.
             */
            scrollId?: string;
            /** @description Return only results with content matching the specified query. */
            query?: string;
            /** @description Fallback to existing language if language is specified. */
            fallback?: boolean;
            /** @description The ISO 639-1 language code describing language. */
            language?: string;
            /** @description Return only results with provided license. */
            license?: string;
            sort?: components["schemas"]["Sort"];
            /** @description Return only learning resources that have one of the provided ids. */
            ids?: number[];
            /** @description A comma separated list of subjects the learning resources should be filtered by. */
            subjects?: string[];
            /** @description Return only learning resources of specific type(s). */
            resourceTypes?: string[];
            /** @description A list of context-types the learning resources should be filtered by. */
            contextTypes?: string[];
            /**
             * @description A list of relevances the learning resources should be filtered by.
             *     If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
             *     If levels are specified the learning resource must have specified relevances in relation to a specified level.
             */
            relevance?: string[];
            /** @description A list of ISO 639-1 language codes that the learning resource can be available in. */
            languageFilter?: string[];
            /** @description A list of codes from GREP API the resources should be filtered by. */
            grepCodes?: string[];
            /** @description A comma separated list of traits the resources should be filtered by. */
            traits?: components["schemas"]["ArticleTrait"][];
            /** @description List of index-paths that should be term-aggregated and returned in result. */
            aggregatePaths?: string[];
            /** @description Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types. */
            embedResource?: string[];
            /** @description Return only results with embed data-resource_id, data-videoid or data-url with the specified id. */
            embedId?: string;
            /** @description Filter out inactive taxonomy contexts. */
            filterInactive?: boolean;
            /** @description Which types the search request should return */
            resultTypes?: components["schemas"]["SearchType"][];
            /** @description Which node types the search request should return */
            nodeTypeFilter?: components["schemas"]["NodeType"][];
            /** @description Only return results that have one of the specified tags. */
            tags?: string[];
        };
        /**
         * SearchSuggestionDTO
         * @description Search suggestion for query-text
         */
        SearchSuggestionDTO: {
            /** @description The search query suggestions are made for */
            text: string;
            /**
             * Format: int32
             * @description The offset in the search query
             */
            offset: number;
            /**
             * Format: int32
             * @description The position index in the search query
             */
            length: number;
            /** @description The list of suggest options for the field */
            options: components["schemas"]["SuggestOptionDTO"][];
        };
        /**
         * SearchType
         * @enum {string}
         */
        SearchType: "article" | "draft" | "learningpath" | "concept" | "grep" | "node";
        /**
         * Sort
         * @description The sorting used on results.
         * @enum {string}
         */
        Sort: "-relevance" | "relevance" | "-title" | "title" | "-lastUpdated" | "lastUpdated" | "-id" | "id" | "-duration" | "duration" | "revisionDate" | "-revisionDate" | "responsibleLastUpdated" | "-responsibleLastUpdated" | "status" | "-status" | "-prioritized" | "prioritized" | "-parentTopicName" | "parentTopicName" | "-primaryRoot" | "primaryRoot" | "-resourceType" | "resourceType" | "-published" | "published" | "-favorited" | "favorited";
        /**
         * StatusDTO
         * @description Status information of the resource
         */
        StatusDTO: {
            /** @description The current status of the resource */
            current: string;
            /** @description Previous statuses this resource has been in */
            other: string[];
        };
        /**
         * SubjectAggregationDTO
         * @description Aggregations for a single subject'
         */
        SubjectAggregationDTO: {
            /** @description Id of the aggregated subject */
            subjectId: string;
            /**
             * Format: int64
             * @description Number of resources in subject
             */
            publishedArticleCount: number;
            /**
             * Format: int64
             * @description Number of resources in subject with published older than 5 years
             */
            oldArticleCount: number;
            /**
             * Format: int64
             * @description Number of resources in subject with a revision date expiration in one year
             */
            revisionCount: number;
            /**
             * Format: int64
             * @description Number of resources in 'flow' (Articles not in `PUBLISHED`, `UNPUBLISHED` or `ARCHIVED` status
             */
            flowCount: number;
            /**
             * Format: int64
             * @description Number of favorited resources
             */
            favoritedCount: number;
        };
        /**
         * SubjectAggregationsDTO
         * @description Result of subject aggregations
         */
        SubjectAggregationsDTO: {
            subjects: components["schemas"]["SubjectAggregationDTO"][];
        };
        /**
         * SubjectAggsInputDTO
         * @description Input parameters to subject aggregations endpoint
         */
        SubjectAggsInputDTO: {
            /** @description A comma separated list of subjects the learning resources should be filtered by. */
            subjects?: string[];
        };
        /**
         * SubjectPageSummaryDTO
         * @description Subject page summary if the node is connected to a subject page
         */
        SubjectPageSummaryDTO: {
            /** Format: int64 */
            id: number;
            name: string;
            /** @description Meta description of the resource */
            metaDescription: components["schemas"]["MetaDescriptionDTO"];
        };
        /**
         * SuggestOptionDTO
         * @description Search suggestion options for the terms in the query
         */
        SuggestOptionDTO: {
            /** @description The suggested text */
            text: string;
            /**
             * Format: double
             * @description The score of the suggestion
             */
            score: number;
        };
        /**
         * TaxonomyResourceTypeDTO
         * @description Taxonomy resource type
         */
        TaxonomyResourceTypeDTO: {
            /** @description Id of the taoxonomy resource type */
            id: string;
            /** @description Name of the taoxonomy resource type */
            name: string;
            /** @description The ISO 639-1 language code for the resource type */
            language: string;
        };
        /**
         * TermValueDTO
         * @description Value that appears in the search aggregation
         */
        TermValueDTO: {
            /** @description Value that appeared in result */
            value: string;
            /**
             * Format: int32
             * @description Number of times the value appeared in result
             */
            count: number;
        };
        /**
         * TitleDTO
         * @description The greps title
         */
        TitleDTO: {
            /** @description The freetext title of the resource */
            title: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
        };
        /**
         * TitleWithHtmlDTO
         * @description The title of the resource
         */
        TitleWithHtmlDTO: {
            /** @description The freetext title of the resource */
            title: string;
            /** @description The freetext html-version title of the article */
            htmlTitle: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
        };
        /**
         * ValidationErrorBody
         * @description Information about an error
         */
        ValidationErrorBody: {
            /** @description Code stating the type of error */
            code: string;
            /** @description Description of the error */
            description: string;
            /** @description When the error occurred */
            occurredAt: string;
            /** @description List of validation messages */
            messages?: components["schemas"]["ValidationMessage"][];
            /**
             * Format: int32
             * @description Numeric http status code
             */
            statusCode: number;
        };
        /**
         * ValidationMessage
         * @description A message describing a validation error on a specific field
         */
        ValidationMessage: {
            /** @description The field the error occured in */
            field: string;
            /** @description The validation message */
            message: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
};
export type $defs = Record<string, never>;
export interface operations {
    "getSearch-apiV1SearchGroup": {
        parameters: {
            query?: {
                page?: number;
                "page-size"?: number;
                "article-types"?: string[];
                "context-types"?: string[];
                language?: string;
                ids?: number[];
                "resource-types"?: string[];
                license?: string;
                query?: string;
                sort?: string;
                fallback?: boolean;
                subjects?: string[];
                "language-filter"?: string[];
                relevance?: string[];
                "search-context"?: string;
                "grep-codes"?: string[];
                "aggregate-paths"?: string[];
                "embed-resource"?: string[];
                "embed-id"?: string;
                "filter-inactive"?: boolean;
                traits?: string[];
                "result-types"?: string[];
                "node-types"?: string[];
                tags?: string[];
                /** @description Whether to include group without resource-types for group-search. Defaults to false. */
                "missing-group"?: boolean;
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GroupSearchResultDTO"][];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getSearch-apiV1Search": {
        parameters: {
            query?: {
                page?: number;
                "page-size"?: number;
                "article-types"?: string[];
                "context-types"?: string[];
                language?: string;
                ids?: number[];
                "resource-types"?: string[];
                license?: string;
                query?: string;
                sort?: string;
                fallback?: boolean;
                subjects?: string[];
                "language-filter"?: string[];
                relevance?: string[];
                "search-context"?: string;
                "grep-codes"?: string[];
                "aggregate-paths"?: string[];
                "embed-resource"?: string[];
                "embed-id"?: string;
                "filter-inactive"?: boolean;
                traits?: string[];
                "result-types"?: string[];
                "node-types"?: string[];
                tags?: string[];
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MultiSearchResultDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "postSearch-apiV1Search": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["SearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MultiSearchResultDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getSearch-apiV1SearchEditorial": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MultiSearchResultDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "postSearch-apiV1SearchEditorial": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["DraftSearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MultiSearchResultDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "postSearch-apiV1SearchSubjects": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectAggsInputDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SubjectAggregationsDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "postSearch-apiV1SearchGrep": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GrepSearchInputDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GrepSearchResultsDTO"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getSearch-apiV1SearchGrepReplacements": {
        parameters: {
            query?: {
                /** @description Grep codes to find replacements for. To provide codes ids, separate by comma (,). */
                codes?: string[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Map_String"];
                };
            };
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorBody"];
                };
            };
        };
    };
}
