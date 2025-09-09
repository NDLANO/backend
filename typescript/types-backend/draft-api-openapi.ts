export type paths = {
    "/draft-api/v1/drafts/licenses": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show all valid licenses
         * @description Shows all valid licenses
         */
        get: operations["getDraft-apiV1DraftsLicenses"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used tags in articles
         * @description Retrieves a list of all previously used tags in articles
         */
        get: operations["getDraft-apiV1DraftsTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/grep-codes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used grepCodes in articles
         * @deprecated
         * @description Retrieves a list of all previously used grepCodes in articles
         */
        get: operations["getDraft-apiV1DraftsGrep-codes"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show all articles
         * @description Shows all articles. You can search it too.
         */
        get: operations["getDraft-apiV1Drafts"];
        put?: never;
        /**
         * Create a new article
         * @description Creates a new article
         */
        post: operations["postDraft-apiV1Drafts"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Show all articles
         * @description Shows all articles. You can search it too.
         */
        post: operations["postDraft-apiV1DraftsSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/status-state-machine": {
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
        get: operations["getDraft-apiV1DraftsStatus-state-machine"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/ids": {
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
        get: operations["getDraft-apiV1DraftsIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show article with a specified Id
         * @description Shows the article for the specified id.
         */
        get: operations["getDraft-apiV1DraftsArticle_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Update an existing article
         * @description Update an existing article
         */
        patch: operations["patchDraft-apiV1DraftsArticle_id"];
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/history": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get all saved articles with a specified Id, latest revision first
         * @description Retrieves all current and previously published articles with the specified id, latest revision first.
         */
        get: operations["getDraft-apiV1DraftsArticle_idHistory"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/revision-history": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the revision history for an article
         * @description Get an object that describes the revision history for a specific article
         */
        get: operations["getDraft-apiV1DraftsArticle_idRevision-history"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/external_id/{deprecated_node_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get internal id of article for a specified ndla_node_id
         * @description Get internal id of article for a specified ndla_node_id
         */
        get: operations["getDraft-apiV1DraftsExternal_idDeprecated_node_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/status/{STATUS}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Update status of an article
         * @description Update status of an article
         */
        put: operations["putDraft-apiV1DraftsArticle_idStatusStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/validate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Validate an article
         * @description Validate an article
         */
        put: operations["putDraft-apiV1DraftsArticle_idValidate"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/language/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * Delete language from article
         * @description Delete language from article
         */
        delete: operations["deleteDraft-apiV1DraftsArticle_idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/clone/{article_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Create a new article with the content of the article with the specified id
         * @description Create a new article with the content of the article with the specified id
         */
        post: operations["postDraft-apiV1DraftsCloneArticle_id"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/partial-publish/{article_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Partial publish selected fields
         * @description Partial publish selected fields
         */
        post: operations["postDraft-apiV1DraftsPartial-publishArticle_id"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/partial-publish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Partial publish selected fields for multiple articles
         * @description Partial publish selected fields for multiple articles
         */
        post: operations["postDraft-apiV1DraftsPartial-publish"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/copyRevisionDates/{node_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Copy revision dates from the node with this id to _all_ children in taxonomy
         * @description Copy revision dates from the node with this id to _all_ children in taxonomy
         */
        post: operations["postDraft-apiV1DraftsCopyrevisiondatesNode_id"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/slug/{slug}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show article with a specified slug
         * @description Shows the article for the specified slug.
         */
        get: operations["getDraft-apiV1DraftsSlugSlug"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/migrate-greps": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Iterate all articles and migrate outdated grep codes
         * @description Iterate all articles and migrate outdated grep codes
         */
        post: operations["postDraft-apiV1DraftsMigrate-greps"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/notes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Add notes to a draft
         * @description Add notes to a draft
         */
        post: operations["postDraft-apiV1DraftsNotes"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/drafts/{article_id}/current-revision": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * Delete the current revision of an article
         * @description Delete the current revision of an article
         */
        delete: operations["deleteDraft-apiV1DraftsArticle_idCurrent-revision"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/files": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Uploads provided file
         * @description Uploads provided file
         */
        post: operations["postDraft-apiV1Files"];
        /**
         * Deletes provided file
         * @description Deletes provided file
         */
        delete: operations["deleteDraft-apiV1Files"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/draft-api/v1/user-data": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves user's data
         * @description Retrieves user's data
         */
        get: operations["getDraft-apiV1User-data"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Update data of logged in user
         * @description Update data of logged in user
         */
        patch: operations["patchDraft-apiV1User-data"];
        trace?: never;
    };
};
export type webhooks = Record<string, never>;
export type components = {
    schemas: {
        /**
         * AddMultipleNotesDTO
         * @description Information about notes to add to drafts
         */
        AddMultipleNotesDTO: {
            /** @description Objects for which notes should be added to which drafts */
            data: components["schemas"]["AddNoteDTO"][];
        };
        /**
         * AddNoteDTO
         * @description Information containing new notes and which draft to add them to
         */
        AddNoteDTO: {
            /**
             * Format: int64
             * @description Id of the draft to add notes to
             */
            draftId: number;
            /** @description Notes to add to the draft */
            notes: string[];
        };
        /** AllErrors */
        AllErrors: components["schemas"]["ErrorBody"] | components["schemas"]["NotFoundWithSupportedLanguages"] | components["schemas"]["ValidationErrorBody"];
        /**
         * ArticleContentDTO
         * @description The content of the article in available languages
         */
        ArticleContentDTO: {
            /** @description The html content */
            content: string;
            /** @description ISO 639-1 code that represents the language used in the content */
            language: string;
        };
        /**
         * ArticleDTO
         * @description Information about the article
         */
        ArticleDTO: {
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
            /** @description The status of this article */
            status: components["schemas"]["StatusDTO"];
            /** @description Available titles for the article */
            title?: components["schemas"]["ArticleTitleDTO"];
            content?: components["schemas"]["ArticleContentDTO"];
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            tags?: components["schemas"]["ArticleTagDTO"];
            /** @description Required libraries in order to render the article */
            requiredLibraries: components["schemas"]["RequiredLibraryDTO"][];
            visualElement?: components["schemas"]["VisualElementDTO"];
            introduction?: components["schemas"]["ArticleIntroductionDTO"];
            metaDescription?: components["schemas"]["ArticleMetaDescriptionDTO"];
            metaImage?: components["schemas"]["ArticleMetaImageDTO"];
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
            /** @description The notes for this article draft */
            notes: components["schemas"]["EditorNoteDTO"][];
            /** @description The labels attached to this article; meant for editors. */
            editorLabels: string[];
            /** @description A list of codes from GREP API connected to the article */
            grepCodes: string[];
            /** @description A list of conceptIds connected to the article */
            conceptIds: number[];
            /** @description Value that dictates who gets to see the article. Possible values are: everyone/teacher */
            availability: string;
            /** @description A list of content related to the article */
            relatedContent: (components["schemas"]["RelatedContentLinkDTO"] | number)[];
            /** @description A list of revisions planned for the article */
            revisions: components["schemas"]["RevisionMetaDTO"][];
            responsible?: components["schemas"]["ResponsibleDTO"];
            /** @description The path to the frontpage article */
            slug?: string;
            /** @description Information about comments attached to the article */
            comments: components["schemas"]["CommentDTO"][];
            priority: components["schemas"]["Priority"];
            /** @description If the article has been edited after last status or responsible change */
            started: boolean;
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            disclaimer?: components["schemas"]["DisclaimerDTO"];
        };
        /**
         * ArticleIntroductionDTO
         * @description An introduction for the article
         */
        ArticleIntroductionDTO: {
            /** @description The introduction content */
            introduction: string;
            /** @description The html introduction content */
            htmlIntroduction: string;
            /** @description The ISO 639-1 language code describing which article translation this introduction belongs to */
            language: string;
        };
        /**
         * ArticleMetaDescriptionDTO
         * @description Meta description for the article
         */
        ArticleMetaDescriptionDTO: {
            /** @description The meta description */
            metaDescription: string;
            /** @description The ISO 639-1 language code describing which article translation this meta description belongs to */
            language: string;
        };
        /**
         * ArticleMetaImageDTO
         * @description Meta image for the article
         */
        ArticleMetaImageDTO: {
            /** @description The meta image */
            url: string;
            /** @description The meta image alt text */
            alt: string;
            /** @description The ISO 639-1 language code describing which article translation this meta image belongs to */
            language: string;
        };
        /**
         * ArticleRevisionHistoryDTO
         * @description Information about article revision history
         */
        ArticleRevisionHistoryDTO: {
            /** @description The revisions of an article, with the latest revision being the first in the list */
            revisions: components["schemas"]["ArticleDTO"][];
            /** @description Whether or not the current revision is safe to delete */
            canDeleteCurrentRevision: boolean;
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
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description Fallback to some existing language if language is specified. */
            fallback?: boolean;
            /** @description Return only articles containing codes from GREP API */
            grepCodes?: string[];
        };
        /**
         * ArticleSearchResultDTO
         * @description Information about search-results
         */
        ArticleSearchResultDTO: {
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
            /** @description The search results */
            results: components["schemas"]["ArticleSummaryDTO"][];
        };
        /**
         * ArticleSummaryDTO
         * @description Short summary of information about the article
         */
        ArticleSummaryDTO: {
            /**
             * Format: int64
             * @description The unique id of the article
             */
            id: number;
            /** @description The title of the article */
            title: components["schemas"]["ArticleTitleDTO"];
            visualElement?: components["schemas"]["VisualElementDTO"];
            introduction?: components["schemas"]["ArticleIntroductionDTO"];
            /** @description The full url to where the complete information about the article can be found */
            url: string;
            /** @description Describes the license of the article */
            license: string;
            /** @description The type of article this is. Possible values are frontpage-article, standard, topic-article */
            articleType: string;
            /** @description A list of available languages for this audio */
            supportedLanguages: string[];
            tags?: components["schemas"]["ArticleTagDTO"];
            /** @description The notes for this draft article */
            notes: string[];
            /** @description The users saved for this draft article */
            users: string[];
            /** @description The codes from GREP API registered for this draft article */
            grepCodes: string[];
            /** @description The status of this article */
            status: components["schemas"]["StatusDTO"];
            /** @description When the article was last updated */
            updated: string;
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
            /** @description The freetext html title of the article */
            htmlTitle: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
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
         * ContentIdDTO
         * @description Id for a single Article
         */
        ContentIdDTO: {
            /**
             * Format: int64
             * @description The unique id of the article
             */
            id: number;
        };
        /**
         * ContributorType
         * @description The description of the author. Eg. Photographer or Supplier
         * @enum {string}
         */
        ContributorType: "artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer";
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
         * DraftCopyrightDTO
         * @description Describes the copyright information for the article
         */
        DraftCopyrightDTO: {
            /** @description Describes the license of the article */
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
            user: string;
            /** @description Status of article at saved time */
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
        /** FileForm */
        FileForm: {
            /** Format: binary */
            file: Blob;
        };
        /**
         * Grade
         * Format: int32
         * @description The grade (1-5) of the article
         * @enum {integer}
         */
        Grade: 1 | 2 | 3 | 4 | 5;
        /**
         * GrepCodesSearchResultDTO
         * @description Information and metadata about codes from GREP API
         */
        GrepCodesSearchResultDTO: {
            /**
             * Format: int64
             * @description The total number of codes from GREP API matching this query
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
            /** @description The search results */
            results: string[];
        };
        /** LicenseDTO */
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
        /**
         * MultiPartialPublishResultDTO
         * @description A list of articles that were partial published to article-api
         */
        MultiPartialPublishResultDTO: {
            /** @description Successful ids */
            successes: number[];
            /** @description Failed ids with error messages */
            failures: components["schemas"]["PartialPublishFailureDTO"][];
        };
        /**
         * NewArticleDTO
         * @description Information about the article
         */
        NewArticleDTO: {
            /** @description The chosen language */
            language: string;
            /** @description The title of the article */
            title: string;
            /** @description The date the article is published */
            published?: string;
            /** @description The content of the article */
            content?: string;
            /** @description Searchable tags */
            tags?: string[];
            /** @description An introduction */
            introduction?: string;
            /** @description A meta description */
            metaDescription?: string;
            /** @description Meta image for the article */
            metaImage?: components["schemas"]["NewArticleMetaImageDTO"];
            /** @description A visual element for the article. May be anything from an image to a video or H5P */
            visualElement?: string;
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            /** @description Required libraries in order to render the article */
            requiredLibraries?: components["schemas"]["RequiredLibraryDTO"][];
            /** @description The type of article this is. Possible values are frontpage-article, standard, topic-article */
            articleType: string;
            /** @description The notes for this article draft */
            notes?: string[];
            /** @description The labels attached to this article; meant for editors. */
            editorLabels?: string[];
            /** @description A list of codes from GREP API connected to the article */
            grepCodes?: string[];
            /** @description A list of conceptIds connected to the article */
            conceptIds?: number[];
            /** @description Value that dictates who gets to see the article. Possible values are: everyone/teacher */
            availability?: string;
            /** @description A list of content related to the article */
            relatedContent?: (components["schemas"]["RelatedContentLinkDTO"] | number)[];
            /** @description An object describing a future revision */
            revisionMeta?: components["schemas"]["RevisionMetaDTO"][];
            /** @description NDLA ID representing the editor responsible for this article */
            responsibleId?: string;
            /** @description The path to the frontpage article */
            slug?: string;
            /** @description Information about a comment attached to an article */
            comments?: components["schemas"]["NewCommentDTO"][];
            priority?: components["schemas"]["Priority"];
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /** @description The disclaimer of the article */
            disclaimer?: string;
        };
        /** NewArticleMetaImageDTO */
        NewArticleMetaImageDTO: {
            /** @description The image-api id of the meta image */
            id: string;
            /** @description The alt text of the meta image */
            alt: string;
        };
        /**
         * NewCommentDTO
         * @description Information about a comment attached to an article
         */
        NewCommentDTO: {
            /** @description Content of the comment */
            content: string;
            /** @description If the comment is open or closed */
            isOpen?: boolean;
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
         * PartialArticleFieldsDTO
         * @enum {string}
         */
        PartialArticleFieldsDTO: "availability" | "grepCodes" | "license" | "metaDescription" | "relatedContent" | "tags" | "revisionDate" | "published";
        /**
         * PartialBulkArticlesDTO
         * @description Partial data about articles to publish in bulk
         */
        PartialBulkArticlesDTO: {
            /** @description A list of article ids to partially publish */
            articleIds: number[];
            /** @description A list of fields that should be partially published */
            fields: components["schemas"]["PartialArticleFieldsDTO"][];
        };
        /**
         * PartialPublishFailureDTO
         * @description Single failed result
         */
        PartialPublishFailureDTO: {
            /**
             * Format: int64
             * @description Id of the article in question
             */
            id: number;
            /** @description Error message */
            message: string;
        };
        /**
         * Priority
         * @description If the article should be prioritized. Possible values are prioritized, on-hold, unspecified
         * @enum {string}
         */
        Priority: "prioritized" | "on-hold" | "unspecified";
        /**
         * QualityEvaluationDTO
         * @description The quality evaluation of the article. Consist of a score from 1 to 5 and a comment.
         */
        QualityEvaluationDTO: {
            grade: components["schemas"]["Grade"];
            /** @description Note explaining the score */
            note?: string;
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
         * ResponsibleDTO
         * @description Object with data representing the editor responsible for this article
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
            /** @description An unique uuid of the revision. If none supplied, one is generated. */
            id?: string;
            /** @description A date on which the article would need to be revised */
            revisionDate: string;
            /** @description Notes to keep track of what needs to happen before revision */
            note: string;
            /** @description Status of a revision, either 'revised' or 'needs-revision' */
            status: string;
        };
        /**
         * SavedSearchDTO
         * @description Information about saved search
         */
        SavedSearchDTO: {
            /** @description The search url */
            searchUrl: string;
            /** @description The search phrase */
            searchPhrase: string;
        };
        /**
         * Sort
         * @description The sorting used on results. Default is by -relevance.
         * @enum {string}
         */
        Sort: "-relevance" | "relevance" | "-title" | "title" | "-lastUpdated" | "lastUpdated" | "-id" | "id";
        /** StatusDTO */
        StatusDTO: {
            /** @description The current status of the article */
            current: string;
            /** @description Previous statuses this article has been in */
            other: string[];
        };
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
         * UpdatedArticleDTO
         * @description Information about the article
         */
        UpdatedArticleDTO: {
            /**
             * Format: int32
             * @description The revision number for the article
             */
            revision: number;
            /** @description The chosen language */
            language?: string;
            /** @description The title of the article */
            title?: string;
            /** @description The status of the article */
            status?: string;
            /** @description The date the article is published */
            published?: string;
            /** @description The content of the article */
            content?: string;
            /** @description Searchable tags */
            tags?: string[];
            /** @description An introduction */
            introduction?: string;
            /** @description A meta description */
            metaDescription?: string;
            /** @description An image-api ID for the article meta image */
            metaImage?: components["schemas"]["NewArticleMetaImageDTO"] | null;
            /** @description A visual element for the article. May be anything from an image to a video or H5P */
            visualElement?: string;
            copyright?: components["schemas"]["DraftCopyrightDTO"];
            /** @description Required libraries in order to render the article */
            requiredLibraries?: components["schemas"]["RequiredLibraryDTO"][];
            /** @description The type of article this is. Possible values are frontpage-article, standard, topic-article */
            articleType?: string;
            /** @description The notes for this article draft */
            notes?: string[];
            /** @description The labels attached to this article; meant for editors. */
            editorLabels?: string[];
            /** @description A list of codes from GREP API connected to the article */
            grepCodes?: string[];
            /** @description A list of conceptIds connected to the article */
            conceptIds?: number[];
            /** @description Stores the new article as a separate version. Useful when making big changes that should be revertable. */
            createNewVersion?: boolean;
            /** @description Value that dictates who gets to see the article. Possible values are: everyone/teacher */
            availability?: string;
            /** @description A list of content related to the article */
            relatedContent?: (components["schemas"]["RelatedContentLinkDTO"] | number)[];
            /** @description A list of all revisions of the article */
            revisionMeta?: components["schemas"]["RevisionMetaDTO"][];
            /** @description NDLA ID representing the editor responsible for this article */
            responsibleId?: string | null;
            /** @description The path to the frontpage article */
            slug?: string;
            /** @description Information about a comment attached to an article */
            comments?: components["schemas"]["UpdatedCommentDTO"][];
            priority?: components["schemas"]["Priority"];
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /** @description The disclaimer of the article */
            disclaimer?: string;
        };
        /**
         * UpdatedCommentDTO
         * @description Information about a comment attached to an article
         */
        UpdatedCommentDTO: {
            /** @description Id of the comment */
            id?: string;
            /** @description Content of the comment */
            content: string;
            /** @description If the comment is open or closed */
            isOpen?: boolean;
            /** @description If the comment is solved or not */
            solved?: boolean;
        };
        /**
         * UpdatedUserDataDTO
         * @description Information about user data
         */
        UpdatedUserDataDTO: {
            /** @description User's saved searches */
            savedSearches?: components["schemas"]["SavedSearchDTO"][];
            /** @description User's last edited articles */
            latestEditedArticles?: string[];
            /** @description User's last edited concepts */
            latestEditedConcepts?: string[];
            /** @description User's last edited learningpaths */
            latestEditedLearningpaths?: string[];
            /** @description User's favorite subjects */
            favoriteSubjects?: string[];
        };
        /**
         * UploadedFileDTO
         * @description Information about the uploaded file
         */
        UploadedFileDTO: {
            /** @description Uploaded file's basename */
            filename: string;
            /** @description Uploaded file's mime type */
            mime: string;
            /** @description Uploaded file's file extension */
            extension: string;
            /** @description Full path of uploaded file */
            path: string;
        };
        /**
         * UserDataDTO
         * @description Information about user data
         */
        UserDataDTO: {
            /** @description The auth0 id of the user */
            userId: string;
            /** @description User's saved searches */
            savedSearches?: components["schemas"]["SavedSearchDTO"][];
            /** @description User's last edited articles */
            latestEditedArticles?: string[];
            /** @description User's last edited concepts */
            latestEditedConcepts?: string[];
            /** @description User's last edited learningpaths */
            latestEditedLearningpaths?: string[];
            /** @description User's favorite subjects */
            favoriteSubjects?: string[];
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
    "getDraft-apiV1DraftsLicenses": {
        parameters: {
            query?: {
                /** @description A filter to remove a specific entry */
                filterNot?: string;
                /** @description A filter to include a specific entry */
                filter?: string;
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
                    "application/json": components["schemas"]["LicenseDTO"][];
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
    "getDraft-apiV1DraftsTag-search": {
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
    "getDraft-apiV1DraftsGrep-codes": {
        parameters: {
            query?: {
                /** @description Return only articles with content matching the specified query. */
                query?: string;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The page number of the search hits to display. */
                page?: number;
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
                    "application/json": components["schemas"]["GrepCodesSearchResultDTO"];
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
    "getDraft-apiV1Drafts": {
        parameters: {
            query?: {
                /** @description Return only articles of specific type(s). To provide multiple types, separate by comma (,). */
                articleTypes?: string[];
                /** @description Return only articles with content matching the specified query. */
                query?: string;
                /** @description Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Return only results with provided license. */
                license?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The sorting used on results.
                 *                  The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description A comma separated list of codes from GREP API the resources should be filtered by. */
                "grep-codes"?: string[];
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
                    "application/json": components["schemas"]["ArticleSearchResultDTO"];
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
    "postDraft-apiV1Drafts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewArticleDTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "postDraft-apiV1DraftsSearch": {
        parameters: {
            query?: never;
            header?: never;
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
                    "application/json": components["schemas"]["ArticleSearchResultDTO"];
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
    "getDraft-apiV1DraftsStatus-state-machine": {
        parameters: {
            query?: {
                /** @description The ID of the article to generate a status state machine for */
                articleId?: number;
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
    "getDraft-apiV1DraftsIds": {
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
                    "application/json": components["schemas"]["ArticleDTO"][];
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
    "getDraft-apiV1DraftsArticle_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
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
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "patchDraft-apiV1DraftsArticle_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedArticleDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ArticleDTO"];
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
            409: {
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
            502: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AllErrors"];
                };
            };
        };
    };
    "getDraft-apiV1DraftsArticle_idHistory": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
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
                    "application/json": components["schemas"]["ArticleDTO"][];
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
    "getDraft-apiV1DraftsArticle_idRevision-history": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
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
                    "application/json": components["schemas"]["ArticleRevisionHistoryDTO"];
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
    "getDraft-apiV1DraftsExternal_idDeprecated_node_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                deprecated_node_id: number;
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
                    "application/json": components["schemas"]["ContentIdDTO"];
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
    "putDraft-apiV1DraftsArticle_idStatusStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
                /** @description An article status */
                STATUS: string;
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
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "putDraft-apiV1DraftsArticle_idValidate": {
        parameters: {
            query?: {
                import_validate?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["UpdatedArticleDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ContentIdDTO"];
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
    "deleteDraft-apiV1DraftsArticle_idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
                /** @description The ISO 639-1 language code describing language. */
                language: string;
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
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "postDraft-apiV1DraftsCloneArticle_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description Add a string to the title marking this article as a copy, defaults to 'true'. */
                "copied-title-postfix"?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
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
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "postDraft-apiV1DraftsPartial-publishArticle_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PartialArticleFieldsDTO"][];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "postDraft-apiV1DraftsPartial-publish": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PartialBulkArticlesDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MultiPartialPublishResultDTO"];
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
    "postDraft-apiV1DraftsCopyrevisiondatesNode_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the taxonomy node to process */
                node_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
    "getDraft-apiV1DraftsSlugSlug": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Slug of the article that is to be fetched. */
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
                    "application/json": components["schemas"]["ArticleDTO"];
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
    "postDraft-apiV1DraftsMigrate-greps": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
    "postDraft-apiV1DraftsNotes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AddMultipleNotesDTO"];
            };
        };
        responses: {
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
            409: {
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
    "deleteDraft-apiV1DraftsArticle_idCurrent-revision": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the article that is to be fetched */
                article_id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
            422: {
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
    "postDraft-apiV1Files": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["FileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UploadedFileDTO"];
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
    "deleteDraft-apiV1Files": {
        parameters: {
            query?: {
                /** @description Path to file. Eg: resources/awdW2CaX.png */
                path?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
    "getDraft-apiV1User-data": {
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
                    "application/json": components["schemas"]["UserDataDTO"];
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
    "patchDraft-apiV1User-data": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedUserDataDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserDataDTO"];
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
}
