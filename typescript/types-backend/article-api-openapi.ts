export type paths = {
    "/article-api/v2/articles/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch tags used in articles.
         * @description Retrieves a list of all previously used tags in articles.
         */
        get: operations["getArticle-apiV2ArticlesTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch articles that matches ids parameter.
         * @description Returns articles that matches ids parameter.
         */
        get: operations["getArticle-apiV2ArticlesIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/{slug}/rss.xml": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get RSS feed for articles at a level in the frontpage menu */
        get: operations["getArticle-apiV2ArticlesSlugRss.xml"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/{article_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getArticle-apiV2ArticlesArticle_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find published articles.
         * @description Returns all articles. You can search it too.
         */
        get: operations["getArticle-apiV2Articles"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find published articles.
         * @description Search all articles.
         */
        post: operations["postArticle-apiV2ArticlesSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/{article_id}/revisions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch list of existing revisions for article-id
         * @description Fetch list of existing revisions for article-id
         */
        get: operations["getArticle-apiV2ArticlesArticle_idRevisions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/external_id/{deprecated_node_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get id of article corresponding to specified deprecated node id.
         * @description Get internal id of article for a specified ndla_node_id.
         */
        get: operations["getArticle-apiV2ArticlesExternal_idDeprecated_node_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/article-api/v2/articles/external_ids/{deprecated_node_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get all ids related to article corresponding to specified deprecated node id.
         * @description Get internal id as well as all deprecated ndla_node_ids of article for a specified ndla_node_id.
         */
        get: operations["getArticle-apiV2ArticlesExternal_idsDeprecated_node_id"];
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
        /**
         * ArticleContentV2DTO
         * @description The content of the article in available languages
         */
        ArticleContentV2DTO: {
            /** @description The html content */
            content: string;
            /** @description ISO 639-1 code that represents the language used in the content */
            language: string;
        };
        /**
         * ArticleIdV2DTO
         * @description Id for a single Article
         */
        ArticleIdV2DTO: {
            /**
             * Format: int64
             * @description The unique id of the article
             */
            id: number;
        };
        /** ArticleIdsDTO */
        ArticleIdsDTO: {
            /** Format: int64 */
            articleId: number;
            externalIds: string[];
        };
        /**
         * ArticleIntroductionDTO
         * @description An introduction for the article
         */
        ArticleIntroductionDTO: {
            /** @description The introduction content */
            introduction: string;
            /** @description The html-version introduction content */
            htmlIntroduction: string;
            /** @description The ISO 639-1 language code describing which article translation this introduction belongs to */
            language: string;
        };
        /** ArticleMetaDescriptionDTO */
        ArticleMetaDescriptionDTO: {
            /** @description The meta description */
            metaDescription: string;
            /** @description The ISO 639-1 language code describing which article translation this meta description belongs to */
            language: string;
        };
        /**
         * ArticleMetaImageDTO
         * @description A meta image for the article
         */
        ArticleMetaImageDTO: {
            /** @description The meta image url */
            url: string;
            /** @description The alt text for the meta image */
            alt: string;
            /** @description The ISO 639-1 language code describing which article translation this meta description belongs to */
            language: string;
        };
        /**
         * ArticleSearchParamsDTO
         * @description The search parameters
         */
        ArticleSearchParamsDTO: {
            /** @description The search query */
            query?: string;
            /** @description The ISO 639-1 language code describing language used in query-params */
            language?: string;
            /** @description Return only articles with provided license. */
            license?: string;
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
            /** @description Return only articles that have one of the provided ids */
            ids?: number[];
            /** @description Return only articles of specific type(s) */
            articleTypes?: string[];
            sort?: components["schemas"]["Sort"];
            /** @description Return all matched articles whether they exist on selected language or not. */
            fallback?: boolean;
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description A comma separated list of codes from GREP API to filter by. */
            grepCodes?: string[];
        };
        /**
         * ArticleSummaryV2DTO
         * @description Short summary of information about the article
         */
        ArticleSummaryV2DTO: {
            /**
             * Format: int64
             * @description The unique id of the article
             */
            id: number;
            /** @description The title of the article */
            title: components["schemas"]["ArticleTitleDTO"];
            visualElement?: components["schemas"]["VisualElementDTO"];
            introduction?: components["schemas"]["ArticleIntroductionDTO"];
            /** @description A metaDescription for the article */
            metaDescription?: components["schemas"]["ArticleMetaDescriptionDTO"];
            metaImage?: components["schemas"]["ArticleMetaImageDTO"];
            /** @description The full url to where the complete information about the article can be found */
            url: string;
            /** @description Describes the license of the article */
            license: string;
            /** @description The type of article this is. Possible values are frontpage-article, standard, topic-article */
            articleType: string;
            /** @description The time when the article was last updated */
            lastUpdated: string;
            /** @description A list of available languages for this article */
            supportedLanguages: string[];
            /** @description A list of codes from GREP API attached to this article */
            grepCodes: string[];
            /** @description Value that dictates who gets to see the article. Possible values are: everyone/teacher */
            availability: string;
        };
        /**
         * ArticleTagDTO
         * @description Searchable tags for the article
         */
        ArticleTagDTO: {
            /** @description The searchable tag. */
            tags: string[];
            /** @description ISO 639-1 code that represents the language used in tag */
            language: string;
        };
        /** ArticleTitleDTO */
        ArticleTitleDTO: {
            /** @description The freetext title of the article */
            title: string;
            /** @description The freetext html-version title of the article */
            htmlTitle: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
        };
        /**
         * ArticleV2DTO
         * @description Information about the article
         */
        ArticleV2DTO: {
            /**
             * Format: int64
             * @description The unique id of the article
             */
            id: number;
            /** @description Link to article on old platform */
            oldNdlaUrl?: string;
            /**
             * Format: int32
             * @description The revision number for the article
             */
            revision: number;
            /** @description Available titles for the article */
            title: components["schemas"]["ArticleTitleDTO"];
            content: components["schemas"]["ArticleContentV2DTO"];
            copyright: components["schemas"]["CopyrightDTO"];
            tags: components["schemas"]["ArticleTagDTO"];
            /** @description Required libraries in order to render the article */
            requiredLibraries: components["schemas"]["RequiredLibraryDTO"][];
            visualElement?: components["schemas"]["VisualElementDTO"];
            metaImage?: components["schemas"]["ArticleMetaImageDTO"];
            introduction?: components["schemas"]["ArticleIntroductionDTO"];
            /** @description Meta description for the article */
            metaDescription: components["schemas"]["ArticleMetaDescriptionDTO"];
            /** @description When the article was created */
            created: string;
            /** @description When the article was last updated */
            updated: string;
            /** @description By whom the article was last updated */
            updatedBy: string;
            /** @description When the article was last published */
            published: string;
            /** @description The type of article this is. Possible values are frontpage-article, standard, topic-article */
            articleType: string;
            /** @description The languages this article supports */
            supportedLanguages: string[];
            /** @description A list of codes from GREP API connected to the article */
            grepCodes: string[];
            /** @description A list of conceptIds connected to the article */
            conceptIds: number[];
            /** @description Value that dictates who gets to see the article. Possible values are: everyone/teacher */
            availability: string;
            /** @description A list of content related to the article */
            relatedContent: (components["schemas"]["RelatedContentLinkDTO"] | number)[];
            /** @description The date for the next planned revision which indicates when the article might be outdated */
            revisionDate?: string;
            /** @description The path to the frontpage article */
            slug?: string;
            disclaimer?: components["schemas"]["DisclaimerDTO"];
        };
        /**
         * AuthorDTO
         * @description Information about an author
         */
        AuthorDTO: {
            type: components["schemas"]["ContributorType"];
            /** @description The name of the of the author */
            name: string;
        };
        /**
         * ContributorType
         * @description The description of the author. Eg. Photographer or Supplier
         * @enum {string}
         */
        ContributorType: "artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer";
        /**
         * CopyrightDTO
         * @description Describes the copyright information for the article
         */
        CopyrightDTO: {
            license: components["schemas"]["LicenseDTO"];
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
         * DisclaimerDTO
         * @description The disclaimer of the article
         */
        DisclaimerDTO: {
            /** @description The freetext html content of the disclaimer */
            disclaimer: string;
            /** @description ISO 639-1 code that represents the language used in the disclaimer */
            language: string;
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
         * RelatedContentLinkDTO
         * @description External link related to the article
         */
        RelatedContentLinkDTO: {
            /** @description Title of the article */
            title: string;
            /** @description The url to where the article can be viewed */
            url: string;
        };
        /**
         * RequiredLibraryDTO
         * @description Information about a library required to render the article
         */
        RequiredLibraryDTO: {
            /** @description The type of the library. E.g. CSS or JavaScript */
            mediaType: string;
            /** @description The name of the library */
            name: string;
            /** @description The full url to where the library can be downloaded */
            url: string;
        };
        /**
         * SearchResultV2DTO
         * @description Information about search-results
         */
        SearchResultV2DTO: {
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
            results: components["schemas"]["ArticleSummaryV2DTO"][];
        };
        /**
         * Sort
         * @description The sorting used on results. Default is by -relevance.
         * @enum {string}
         */
        Sort: "-relevance" | "relevance" | "-title" | "title" | "-lastUpdated" | "lastUpdated" | "-id" | "id";
        /**
         * TagsSearchResultDTO
         * @description Information about tags-search-results
         */
        TagsSearchResultDTO: {
            /**
             * Format: int64
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
         * @description A visual element article
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
    "getArticle-apiV2ArticlesTag-search": {
        parameters: {
            query?: {
                /** @description Return only articles with content matching the specified query. */
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
    "getArticle-apiV2ArticlesIds": {
        parameters: {
            query?: {
                /** @description Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The page number of the search hits to display. */
                page?: number;
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
                    "application/json": components["schemas"]["ArticleV2DTO"][];
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
    "getArticle-apiV2ArticlesSlugRss.xml": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Slug of the article to generate RSS for */
                slug: string;
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
                    "text/plain": string;
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
            410: {
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
    "getArticle-apiV2ArticlesArticle_id": {
        parameters: {
            query?: {
                /** @description Revision of article to fetch. If not provided the current revision is returned. */
                revision?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description Id or slug of the article that is to be fetched. */
                article_id: string;
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
                    "application/json": components["schemas"]["ArticleV2DTO"];
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
            410: {
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
    "getArticle-apiV2Articles": {
        parameters: {
            query?: {
                /** @description Return only articles with content matching the specified query. */
                query?: string;
                /** @description Return only articles of specific type(s). To provide multiple types, separate by comma (,). */
                articleTypes?: string[];
                /** @description Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Return only results with provided license. Specifying 'all' gives all articles regardless of licence. */
                license?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and id (asc) when query is empty. */
                sort?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description A comma separated list of codes from GREP API the resources should be filtered by. */
                "grep-codes"?: string[];
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
                    "application/json": components["schemas"]["SearchResultV2DTO"];
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
    "postArticle-apiV2ArticlesSearch": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ArticleSearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SearchResultV2DTO"];
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
    "getArticle-apiV2ArticlesArticle_idRevisions": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id or slug of the article that is to be fetched. */
                article_id: number;
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
                    "application/json": number[];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getArticle-apiV2ArticlesExternal_idDeprecated_node_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of deprecated NDLA node */
                deprecated_node_id: string;
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
                    "application/json": components["schemas"]["ArticleIdV2DTO"];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getArticle-apiV2ArticlesExternal_idsDeprecated_node_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of deprecated NDLA node */
                deprecated_node_id: string;
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
                    "application/json": components["schemas"]["ArticleIdsDTO"];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
}
