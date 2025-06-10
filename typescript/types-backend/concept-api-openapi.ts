export type paths = {
    "/concept-api/v1/drafts/status-state-machine": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get status state machine
         * @description Get status state machine
         */
        get: operations["getConcept-apiV1DraftsStatus-state-machine"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/drafts/tags": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Returns a list of all tags in the specified subjects
         * @description Returns a list of all tags in the specified subjects
         */
        get: operations["getConcept-apiV1DraftsTags"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/drafts/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Show all concepts
         * @description Shows all concepts. You can search it too.
         */
        post: operations["postConcept-apiV1DraftsSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/drafts/{concept_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show concept with a specified id
         * @description Shows the concept for the specified id.
         */
        get: operations["getConcept-apiV1DraftsConcept_id"];
        put?: never;
        post?: never;
        /**
         * Delete language from concept
         * @description Delete language from concept
         */
        delete: operations["deleteConcept-apiV1DraftsConcept_id"];
        options?: never;
        head?: never;
        /**
         * Update a concept
         * @description Update a concept
         */
        patch: operations["patchConcept-apiV1DraftsConcept_id"];
        trace?: never;
    };
    "/concept-api/v1/drafts/{concept_id}/status/{STATUS}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Update status of a concept
         * @description Update status of a concept
         */
        put: operations["putConcept-apiV1DraftsConcept_idStatusStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/drafts/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used tags in concepts
         * @description Retrieves a list of all previously used tags in concepts
         */
        get: operations["getConcept-apiV1DraftsTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/drafts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show all concepts
         * @description Shows all concepts. You can search it too.
         */
        get: operations["getConcept-apiV1Drafts"];
        put?: never;
        /**
         * Create new concept
         * @description Create new concept
         */
        post: operations["postConcept-apiV1Drafts"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/concepts/tags": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Returns a list of all tags in the specified subjects
         * @description Returns a list of all tags in the specified subjects
         */
        get: operations["getConcept-apiV1ConceptsTags"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/concepts/{concept_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show concept with a specified id
         * @description Shows the concept for the specified id.
         */
        get: operations["getConcept-apiV1ConceptsConcept_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/concepts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show all concepts
         * @description Shows all concepts. You can search it too.
         */
        get: operations["getConcept-apiV1Concepts"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/concept-api/v1/concepts/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Show all concepts
         * @description Shows all concepts. You can search it too.
         */
        post: operations["postConcept-apiV1ConceptsSearch"];
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
        /**
         * AuthorDTO
         * @description Information about an author
         */
        AuthorDTO: {
            type: components["schemas"]["ContributorType"];
            /** @description The name of the of the author */
            name: string;
        };
        /** ConceptContent */
        ConceptContent: {
            /** @description The content of this concept */
            content: string;
            /** @description The html content of this concept */
            htmlContent: string;
            /** @description The language of this concept */
            language: string;
        };
        /**
         * ConceptDTO
         * @description Information about the concept
         */
        ConceptDTO: {
            /**
             * Format: int64
             * @description The unique id of the concept
             */
            id: number;
            /**
             * Format: int32
             * @description The revision of the concept
             */
            revision: number;
            title: components["schemas"]["ConceptTitleDTO"];
            /** @description The content of the concept */
            content?: components["schemas"]["ConceptContent"];
            /** @description Describes the copyright information for the concept */
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            /** @description URL for the source of the concept */
            source?: string;
            tags?: components["schemas"]["ConceptTagsDTO"];
            /** @description When the concept was created */
            created: string;
            /** @description When the concept was last updated */
            updated: string;
            /** @description List of people that updated this concept */
            updatedBy?: string[];
            /** @description All available languages of the current concept */
            supportedLanguages: string[];
            /** @description Status information of the concept */
            status: components["schemas"]["StatusDTO"];
            visualElement?: components["schemas"]["VisualElementDTO"];
            responsible?: components["schemas"]["ConceptResponsibleDTO"];
            /** @description Type of concept. 'concept', or 'gloss' */
            conceptType: string;
            glossData?: components["schemas"]["GlossDataDTO"];
            /** @description Describes the changes made to the concept, only visible to editors */
            editorNotes?: components["schemas"]["EditorNoteDTO"][];
        };
        /**
         * ConceptResponsibleDTO
         * @description Object with data representing the editor responsible for this concept
         */
        ConceptResponsibleDTO: {
            /** @description NDLA ID of responsible editor */
            responsibleId: string;
            /** @description Date of when the responsible editor was last updated */
            lastUpdated: string;
        };
        /**
         * ConceptSearchParamsDTO
         * @description The search parameters
         */
        ConceptSearchParamsDTO: {
            /** @description The search query. */
            query?: string;
            /** @description The ISO 639-1 language code describing language used in query-params. */
            language?: string;
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
            /** @description Return only articles that have one of the provided ids. */
            ids?: number[];
            sort?: components["schemas"]["Sort"];
            /** @description Whether to fallback to existing language if not found in selected language. */
            fallback?: boolean;
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description A comma-separated list of tags to filter the search by. */
            tags?: string[];
            /** @description If provided, only return concept where query matches title exactly. */
            exactMatch?: boolean;
            /** @description Embed resource type that should exist in the concepts. */
            embedResource?: string[];
            /** @description Embed id attribute that should exist in the concepts. */
            embedId?: string;
            /** @description The type of concepts to return. */
            conceptType?: string;
            /** @description A list of index paths to aggregate over */
            aggregatePaths?: string[];
        };
        /**
         * ConceptSearchResultDTO
         * @description Information about search-results
         */
        ConceptSearchResultDTO: {
            /**
             * Format: int64
             * @description The total number of articles matching this query
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
            results: components["schemas"]["ConceptSummaryDTO"][];
            /** @description The aggregated fields if specified in query */
            aggregations: components["schemas"]["MultiSearchTermsAggregationDTO"][];
        };
        /**
         * ConceptSummaryDTO
         * @description Information about the concept
         */
        ConceptSummaryDTO: {
            /**
             * Format: int64
             * @description The unique id of the concept
             */
            id: number;
            title: components["schemas"]["ConceptTitleDTO"];
            /** @description The content of the concept in available languages */
            content: components["schemas"]["ConceptContent"];
            tags?: components["schemas"]["ConceptTagsDTO"];
            /** @description All available languages of the current concept */
            supportedLanguages: string[];
            /** @description The time when the article was last updated */
            lastUpdated: string;
            /** @description When the concept was created */
            created: string;
            /** @description Status information of the concept */
            status: components["schemas"]["StatusDTO"];
            /** @description List of people that edited the concept */
            updatedBy: string[];
            /** @description Describes the license of the concept */
            license?: string;
            /** @description Describes the copyright of the concept */
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            visualElement?: components["schemas"]["VisualElementDTO"];
            /** @description URL for the source of the concept */
            source?: string;
            responsible?: components["schemas"]["ConceptResponsibleDTO"];
            /** @description Type of concept. 'concept', or 'gloss' */
            conceptType: string;
            glossData?: components["schemas"]["GlossDataDTO"];
            /** @description A translated name of the concept type */
            conceptTypeName: string;
        };
        /**
         * ConceptTagsDTO
         * @description Search tags the concept is tagged with
         */
        ConceptTagsDTO: {
            /** @description Searchable tags */
            tags: string[];
            /** @description The ISO 639-1 language code describing which concept translation these tags belongs to */
            language: string;
        };
        /**
         * ConceptTitleDTO
         * @description Available titles for the concept
         */
        ConceptTitleDTO: {
            /** @description The title of this concept */
            title: string;
            /** @description The language of this concept */
            language: string;
        };
        /**
         * ContributorType
         * @description The description of the author. Eg. Photographer or Supplier
         * @enum {string}
         */
        ContributorType: "artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer";
        /**
         * DraftConceptSearchParamsDTO
         * @description The search parameters
         */
        DraftConceptSearchParamsDTO: {
            /** @description The search query. */
            query?: string;
            /** @description The ISO 639-1 language code describing language used in query-params. */
            language?: string;
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
            /** @description Return only articles that have one of the provided ids. */
            ids?: number[];
            sort?: components["schemas"]["Sort"];
            /** @description Whether to fallback to existing language if not found in selected language. */
            fallback?: boolean;
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description A comma-separated list of tags to filter the search by. */
            tags?: string[];
            /** @description A comma-separated list of statuses that should appear in the search. */
            status?: string[];
            /** @description A comma-separated list of users to filter the search by. */
            users?: string[];
            /** @description Embed resource type that should exist in the concepts. */
            embedResource?: string[];
            /** @description Embed id attribute that should exist in the concepts. */
            embedId?: string;
            /** @description A comma-separated list of NDLA IDs to filter the search by. */
            responsibleIds?: string[];
            /** @description The type of concepts to return. */
            conceptType?: string;
            /** @description A list of index paths to aggregate over */
            aggregatePaths?: string[];
        };
        /** DraftCopyrightDTO */
        DraftCopyrightDTO: {
            license?: components["schemas"]["LicenseDTO"];
            /** @description Reference to where the article is procured */
            origin?: string;
            /** @description List of creators */
            creators: components["schemas"]["AuthorDTO"][];
            /** @description List of processors */
            processors: components["schemas"]["AuthorDTO"][];
            /** @description List of rightsholders */
            rightsholders: components["schemas"]["AuthorDTO"][];
            /** @description Date from which the copyright is valid */
            validFrom?: string;
            /** @description Date to which the copyright is valid */
            validTo?: string;
            /** @description Whether or not the content has been processed */
            processed: boolean;
        };
        /**
         * EditorNoteDTO
         * @description Information about the editorial notes
         */
        EditorNoteDTO: {
            /** @description Editorial note */
            note: string;
            /** @description User which saved the note */
            updatedBy: string;
            /** @description Status of concept at saved time */
            status: components["schemas"]["StatusDTO"];
            /** @description Timestamp of when note was saved */
            timestamp: string;
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
         * GlossDataDTO
         * @description Information about the gloss
         */
        GlossDataDTO: {
            /** @description The gloss itself */
            gloss: string;
            /** @description Word class / part of speech, ex. noun, adjective, verb, adverb, ... */
            wordClass: string;
            /** @description Original language of the gloss */
            originalLanguage: string;
            /** @description Alternative writing of the gloss */
            transcriptions: components["schemas"]["Map_String"];
            /** @description List of examples of how the gloss can be used */
            examples: components["schemas"]["GlossExampleDTO"][][];
        };
        /**
         * GlossExampleDTO
         * @description Information about the gloss example
         */
        GlossExampleDTO: {
            /** @description Example use of the gloss */
            example: string;
            /** @description Language of the example */
            language: string;
            /** @description Alternative writing of the example */
            transcriptions: components["schemas"]["Map_String"];
        };
        /**
         * LicenseDTO
         * @description Describes the license of the article
         */
        LicenseDTO: {
            /** @description The name of the license */
            license: string;
            /** @description Description of the license */
            description?: string;
            /** @description Url to where the license can be found */
            url?: string;
        };
        /** Map_List_String */
        Map_List_String: {
            [key: string]: string[];
        };
        /** Map_String */
        Map_String: {
            [key: string]: string;
        };
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
        /**
         * NewConceptDTO
         * @description Information about the concept
         */
        NewConceptDTO: {
            /** @description The language of this concept */
            language: string;
            /** @description Available titles for the concept */
            title: string;
            /** @description The content of the concept */
            content?: string;
            /** @description Describes the copyright information for the concept */
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            /** @description A list of searchable tags */
            tags?: string[];
            /** @description A visual element for the concept. May be anything from an image to a video or H5P */
            visualElement?: string;
            /** @description NDLA ID representing the editor responsible for this article */
            responsibleId?: string;
            /** @description Type of concept. 'concept', or 'gloss' */
            conceptType: string;
            glossData?: components["schemas"]["GlossDataDTO"];
        };
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
         * Sort
         * @description The sorting used on results. Default is by -relevance.
         * @enum {string}
         */
        Sort: "-relevance" | "relevance" | "-title" | "title" | "-lastUpdated" | "lastUpdated" | "-id" | "id" | "-responsibleLastUpdated" | "responsibleLastUpdated" | "status" | "-status" | "subject" | "-subject" | "conceptType" | "-conceptType";
        /** StatusDTO */
        StatusDTO: {
            /** @description The current status of the concept */
            current: string;
            /** @description Previous statuses this concept has been in */
            other: string[];
        };
        /**
         * TagsSearchResultDTO
         * @description Information about tags-search-results
         */
        TagsSearchResultDTO: {
            /**
             * Format: int32
             * @description The total number of tags matching this query
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
            results: string[];
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
         * UpdatedConceptDTO
         * @description Information about the concept
         */
        UpdatedConceptDTO: {
            /** @description The language of this concept */
            language: string;
            /** @description Available titles for the concept */
            title?: string;
            /** @description The content of the concept */
            content?: string;
            /** @description Describes the copyright information for the concept */
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            /** @description A list of searchable tags */
            tags?: string[];
            /** @description The new status of the concept */
            status?: string;
            /** @description A visual element for the concept. May be anything from an image to a video or H5P */
            visualElement?: string;
            /** @description NDLA ID representing the editor responsible for this article */
            responsibleId?: string | null;
            /** @description Type of concept. 'concept', or 'gloss' */
            conceptType?: string;
            glossData?: components["schemas"]["GlossDataDTO"];
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
        /**
         * VisualElementDTO
         * @description A visual element for the concept
         */
        VisualElementDTO: {
            /** @description Html containing the visual element. May contain any legal html element, including the embed-tag */
            visualElement: string;
            /** @description The ISO 639-1 language code describing which article translation this visual element belongs to */
            language: string;
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
    "getConcept-apiV1DraftsStatus-state-machine": {
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
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Map_List_String"];
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
    "getConcept-apiV1DraftsTags": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": string[];
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
    "postConcept-apiV1DraftsSearch": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["DraftConceptSearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptSearchResultDTO"];
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
    "getConcept-apiV1DraftsConcept_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the concept that is to be returned */
                concept_id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "deleteConcept-apiV1DraftsConcept_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Id of the concept that is to be returned */
                concept_id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "patchConcept-apiV1DraftsConcept_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the concept that is to be returned */
                concept_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedConceptDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "putConcept-apiV1DraftsConcept_idStatusStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the concept that is to be returned */
                concept_id: number;
                /** @description Concept status */
                STATUS: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "getConcept-apiV1DraftsTag-search": {
        parameters: {
            query?: {
                /** @description Return only concepts with content matching the specified query. */
                query?: string;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["TagsSearchResultDTO"];
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
    "getConcept-apiV1Drafts": {
        parameters: {
            query?: {
                /** @description Return only concepts with content matching the specified query. */
                query?: string;
                /** @description Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The sorting used on results.
                 *                  The following are supported: ByRelevanceDesc,ByRelevanceAsc,ByTitleDesc,ByTitleAsc,ByLastUpdatedDesc,ByLastUpdatedAsc,ByIdDesc,ByIdAsc,ByResponsibleLastUpdatedDesc,ByResponsibleLastUpdatedAsc,ByStatusAsc,ByStatusDesc,BySubjectAsc,BySubjectDesc,ByConceptTypeAsc,ByConceptTypeDesc
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result.
                 *     If you are not paginating very far, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description A comma-separated list of tags to filter the search by. */
                tags?: string[];
                /** @description List of statuses to filter by.
                 *     A draft only needs to have one of the available statuses to appear in result (OR).
                 *             */
                status?: string[];
                /** @description List of users to filter by.
                 *     The value to search for is the user-id from Auth0. */
                users?: string[];
                /** @description Return concepts with matching embed type. */
                "embed-resource"?: string[];
                /** @description Return concepts with matching embed id. */
                "embed-id"?: string;
                /** @description List of responsible ids to filter by (OR filter) */
                "responsible-ids"?: string[];
                /** @description Return only concepts of given type. Allowed values are concept,gloss */
                "concept-type"?: string;
                /** @description List of index-paths that should be term-aggregated and returned in result. */
                "aggregate-paths"?: string[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptSearchResultDTO"];
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
    "postConcept-apiV1Drafts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewConceptDTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    "Cache-Control": string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "getConcept-apiV1ConceptsTags": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
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
                    "application/json": string[];
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
    "getConcept-apiV1ConceptsConcept_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the concept that is to be returned */
                concept_id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptDTO"];
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
    "getConcept-apiV1Concepts": {
        parameters: {
            query?: {
                /** @description Return only concepts with content matching the specified query. */
                query?: string;
                /** @description Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The sorting used on results.
                 *                  The following are supported: ByRelevanceDesc,ByRelevanceAsc,ByTitleDesc,ByTitleAsc,ByLastUpdatedDesc,ByLastUpdatedAsc,ByIdDesc,ByIdAsc,ByResponsibleLastUpdatedDesc,ByResponsibleLastUpdatedAsc,ByStatusAsc,ByStatusDesc,BySubjectAsc,BySubjectDesc,ByConceptTypeAsc,ByConceptTypeDesc
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result.
                 *     If you are not paginating very far, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description A comma-separated list of tags to filter the search by. */
                tags?: string[];
                /** @description If provided, only return concept where query matches title exactly. */
                "exact-match"?: boolean;
                /** @description Return concepts with matching embed type. */
                "embed-resource"?: string[];
                /** @description Return concepts with matching embed id. */
                "embed-id"?: string;
                /** @description Return only concepts of given type. Allowed values are concept,gloss */
                "concept-type"?: string;
                /** @description List of index-paths that should be term-aggregated and returned in result. */
                "aggregate-paths"?: string[];
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
                    "application/json": components["schemas"]["ConceptSearchResultDTO"];
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
    "postConcept-apiV1ConceptsSearch": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConceptSearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConceptSearchResultDTO"];
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
}
