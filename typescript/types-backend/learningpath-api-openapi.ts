export type paths = {
    "/learningpath-api/v2/learningpaths": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find public learningpaths
         * @description Show public learningpaths.
         */
        get: operations["getLearningpath-apiV2Learningpaths"];
        put?: never;
        /**
         * Store new learningpath
         * @description Adds the given learningpath
         */
        post: operations["postLearningpath-apiV2Learningpaths"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find public learningpaths
         * @description Show public learningpaths
         */
        post: operations["postLearningpath-apiV2LearningpathsSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/tags": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch all previously used tags in learningpaths
         * @description Retrieves a list of all previously used tags in learningpaths
         */
        get: operations["getLearningpath-apiV2LearningpathsTags"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/licenses": {
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
        get: operations["getLearningpath-apiV2LearningpathsLicenses"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/mine": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch all learningspaths you have created
         * @description Shows your learningpaths.
         */
        get: operations["getLearningpath-apiV2LearningpathsMine"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/contributors": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch all previously used contributors in learningpaths
         * @description Retrieves a list of all previously used contributors in learningpaths
         */
        get: operations["getLearningpath-apiV2LearningpathsContributors"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch learningpaths that matches ids parameter.
         * @description Returns learningpaths that matches ids parameter.
         */
        get: operations["getLearningpath-apiV2LearningpathsIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch details about the specified learningpath
         * @description Shows all information about the specified learningpath.
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_id"];
        put?: never;
        post?: never;
        /**
         * Delete given learningpath
         * @description Deletes the given learningPath
         */
        delete: operations["deleteLearningpath-apiV2LearningpathsLearningpath_id"];
        options?: never;
        head?: never;
        /**
         * Update given learningpath
         * @description Updates the given learningPath
         */
        patch: operations["patchLearningpath-apiV2LearningpathsLearningpath_id"];
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show status information for the learningpath
         * @description Shows publishingstatus for the learningpath
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_idStatus"];
        /**
         * Update status of given learningpath
         * @description Updates the status of the given learningPath
         */
        put: operations["putLearningpath-apiV2LearningpathsLearningpath_idStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps/trash": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch deleted learningsteps for given learningpath
         * @description Show all learningsteps for the given learningpath that are marked as deleted
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsTrash"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch learningsteps for given learningpath
         * @description Show all learningsteps for given learningpath id
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_idLearningsteps"];
        put?: never;
        /**
         * Add new learningstep to learningpath
         * @description Adds the given LearningStep
         */
        post: operations["postLearningpath-apiV2LearningpathsLearningpath_idLearningsteps"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps/{learningstep_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch details about the specified learningstep
         * @description Show the given learningstep for the given learningpath
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id"];
        put?: never;
        post?: never;
        /**
         * Delete given learningstep
         * @description Deletes the given learningStep
         */
        delete: operations["deleteLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id"];
        options?: never;
        head?: never;
        /**
         * Update given learningstep
         * @description Update the given learningStep
         */
        patch: operations["patchLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id"];
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/contains-article/{article_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch learningpaths containing specified article
         * @description Fetch learningpaths containing specified article
         */
        get: operations["getLearningpath-apiV2LearningpathsContains-articleArticle_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps/{learningstep_id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Show status information for learningstep
         * @description Shows status for the learningstep
         */
        get: operations["getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idStatus"];
        /**
         * Update status of given learningstep
         * @description Updates the status of the given learningstep
         */
        put: operations["putLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/copy": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Copy given learningpath and store it as a new learningpath
         * @description Copies the given learningpath, with the option to override some fields
         */
        post: operations["postLearningpath-apiV2LearningpathsLearningpath_idCopy"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps/{learningstep_id}/seqNo": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Store new sequence number for learningstep.
         * @description Updates the sequence number for the given learningstep. The sequence number of other learningsteps will be affected by this.
         */
        put: operations["putLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idSeqno"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/learningsteps/{learningstep_id}/language/{p1}": {
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
         * Delete given learningstep language
         * @description Deletes the given learningStep language
         */
        delete: operations["deleteLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idLanguageP1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/status/{STATUS}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch all learningpaths with specified status
         * @description Fetch all learningpaths with specified status
         */
        get: operations["getLearningpath-apiV2LearningpathsStatusStatus"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/language/{p1}": {
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
         * Delete the given language of a learning path
         * @description Delete the given language of a learning path
         */
        delete: operations["deleteLearningpath-apiV2LearningpathsLearningpath_idLanguageP1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v2/learningpaths/{learningpath_id}/update-taxonomy": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Update taxonomy for specified learningpath
         * @description Update taxonomy for specified learningpath
         */
        post: operations["postLearningpath-apiV2LearningpathsLearningpath_idUpdate-taxonomy"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/learningpath-api/v1/stats": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get stats for my-ndla usage.
         * @deprecated
         * @description Get stats for my-ndla usage.
         */
        get: operations["getLearningpath-apiV1Stats"];
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
         * ContributorType
         * @description The description of the author. Eg. Photographer or Supplier
         * @enum {string}
         */
        ContributorType: "artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer";
        /** CopyrightDTO */
        CopyrightDTO: {
            /** @description Describes the license of the learningpath */
            license: components["schemas"]["LicenseDTO"];
            /** @description List of authors */
            contributors: components["schemas"]["AuthorDTO"][];
        };
        /**
         * CoverPhotoDTO
         * @description Information about where the cover photo can be found
         */
        CoverPhotoDTO: {
            url: string;
            metaUrl: string;
        };
        /** DescriptionDTO */
        DescriptionDTO: {
            /** @description The learningpath description. Basic HTML allowed */
            description: string;
            /** @description ISO 639-1 code that represents the language used in description */
            language: string;
        };
        /**
         * EmbedUrlV2DTO
         * @description The embed content for the learningstep
         */
        EmbedUrlV2DTO: {
            /** @description The url */
            url: string;
            /** @description Type of embed content */
            embedType: string;
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
        /** IntroductionDTO */
        IntroductionDTO: {
            /** @description The introduction to the learningpath. Basic HTML allowed */
            introduction: string;
            /** @description ISO 639-1 code that represents the language used in introduction */
            language: string;
        };
        /**
         * LearningPathStatusDTO
         * @description Status information about a learningpath
         */
        LearningPathStatusDTO: {
            /** @description The publishing status of the learningpath */
            status: string;
        };
        /**
         * LearningPathSummaryV2DTO
         * @description Summary of meta information for a learningpath
         */
        LearningPathSummaryV2DTO: {
            /**
             * Format: int64
             * @description The unique id of the learningpath
             */
            id: number;
            /**
             * Format: int32
             * @description The revision number for this learningpath
             */
            revision?: number;
            /** @description The titles of the learningpath */
            title: components["schemas"]["TitleDTO"];
            /** @description The descriptions of the learningpath */
            description: components["schemas"]["DescriptionDTO"];
            /** @description The introductions of the learningpath */
            introduction: components["schemas"]["IntroductionDTO"];
            /** @description The full url to where the complete metainformation about the learningpath can be found */
            metaUrl: string;
            /** @description Url to where a cover photo can be found */
            coverPhotoUrl?: string;
            /**
             * Format: int32
             * @description The duration of the learningpath in minutes
             */
            duration?: number;
            /** @description The publishing status of the learningpath. */
            status: string;
            /** @description The date when this learningpath was created. */
            created: string;
            /** @description The date when this learningpath was last updated. */
            lastUpdated: string;
            tags: components["schemas"]["LearningPathTagsDTO"];
            /** @description The contributors of this learningpath */
            copyright: components["schemas"]["CopyrightDTO"];
            /** @description A list of available languages for this audio */
            supportedLanguages: string[];
            /**
             * Format: int64
             * @description The id this learningpath is based on, if any
             */
            isBasedOn?: number;
            /** @description Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath */
            message?: string;
        };
        /**
         * LearningPathTagsDTO
         * @description Searchable tags for the learningpath
         */
        LearningPathTagsDTO: {
            /** @description The searchable tags. Must be plain text */
            tags: string[];
            /** @description ISO 639-1 code that represents the language used in tag */
            language: string;
        };
        /** LearningPathTagsSummaryDTO */
        LearningPathTagsSummaryDTO: {
            /** @description The chosen language. Default is 'nb' */
            language: string;
            /** @description The supported languages for these tags */
            supportedLanguages: string[];
            /** @description The searchable tags. Must be plain text */
            tags: string[];
        };
        /**
         * LearningPathV2DTO
         * @description Meta information for a learningpath
         */
        LearningPathV2DTO: {
            /**
             * Format: int64
             * @description The unique id of the learningpath
             */
            id: number;
            /**
             * Format: int32
             * @description The revision number for this learningpath
             */
            revision: number;
            /**
             * Format: int64
             * @description The id this learningpath is based on, if any
             */
            isBasedOn?: number;
            /** @description The title of the learningpath */
            title: components["schemas"]["TitleDTO"];
            /** @description The description of the learningpath */
            description: components["schemas"]["DescriptionDTO"];
            /** @description The full url to where the complete metainformation about the learningpath can be found */
            metaUrl: string;
            /** @description The learningsteps-summaries for this learningpath */
            learningsteps: components["schemas"]["LearningStepV2DTO"][];
            /** @description The full url to where the learningsteps can be found */
            learningstepUrl: string;
            coverPhoto?: components["schemas"]["CoverPhotoDTO"];
            /**
             * Format: int32
             * @description The duration of the learningpath in minutes
             */
            duration?: number;
            /** @description The publishing status of the learningpath */
            status: string;
            /** @description Verification status */
            verificationStatus: string;
            /** @description The date when this learningpath was created. */
            created: string;
            /** @description The date when this learningpath was last updated. */
            lastUpdated: string;
            tags: components["schemas"]["LearningPathTagsDTO"];
            /** @description Describes the copyright information for the learningpath */
            copyright: components["schemas"]["CopyrightDTO"];
            /** @description True if authenticated user may edit this learningpath */
            canEdit: boolean;
            /** @description The supported languages for this learningpath */
            supportedLanguages: string[];
            /** @description Visible if administrator or owner of LearningPath */
            ownerId?: string;
            message?: components["schemas"]["MessageDTO"];
            /** @description The date when this learningpath was made available to the public. */
            madeAvailable?: string;
            /** @description Whether the owner of the learningpath is a MyNDLA user or not */
            isMyNDLAOwner: boolean;
            responsible?: components["schemas"]["ResponsibleDTO"];
            /** @description Information about comments attached to the learningpath */
            comments: components["schemas"]["CommentDTO"][];
            priority: components["schemas"]["Priority"];
            /** @description A list of revisions planned for the learningpath */
            revisions: components["schemas"]["RevisionMetaDTO"][];
        };
        /**
         * LearningStepContainerSummaryDTO
         * @description Summary of meta information for a learningstep including language and supported languages
         */
        LearningStepContainerSummaryDTO: {
            /** @description The chosen search language */
            language: string;
            /** @description The chosen search language */
            learningsteps: components["schemas"]["LearningStepSummaryV2DTO"][];
            /** @description The chosen search language */
            supportedLanguages: string[];
        };
        /**
         * LearningStepSeqNoDTO
         * @description Information about the sequence number for a step
         */
        LearningStepSeqNoDTO: {
            /**
             * Format: int32
             * @description The sequence number for the learningstep
             */
            seqNo: number;
        };
        /**
         * LearningStepStatusDTO
         * @description Status information about a learningpath
         */
        LearningStepStatusDTO: {
            /** @description The status of the learningstep */
            status: string;
        };
        /**
         * LearningStepSummaryV2DTO
         * @description Summary of meta information for a learningstep
         */
        LearningStepSummaryV2DTO: {
            /**
             * Format: int64
             * @description The id of the learningstep
             */
            id: number;
            /**
             * Format: int32
             * @description The sequence number for the step. The first step has seqNo 0.
             */
            seqNo: number;
            /** @description The title of the learningstep */
            title: components["schemas"]["TitleDTO"];
            /** @description The type of the step */
            type: string;
            /** @description The full url to where the complete metainformation about the learningstep can be found */
            metaUrl: string;
        };
        /**
         * LearningStepV2DTO
         * @description Information about a learningstep
         */
        LearningStepV2DTO: {
            /**
             * Format: int64
             * @description The id of the learningstep
             */
            id: number;
            /**
             * Format: int32
             * @description The revision number for this learningstep
             */
            revision: number;
            /**
             * Format: int32
             * @description The sequence number for the step. The first step has seqNo 0.
             */
            seqNo: number;
            /** @description The title of the learningstep */
            title: components["schemas"]["TitleDTO"];
            /** @description The introduction of the learningstep */
            introduction?: components["schemas"]["IntroductionDTO"];
            /** @description The description of the learningstep */
            description?: components["schemas"]["DescriptionDTO"];
            embedUrl?: components["schemas"]["EmbedUrlV2DTO"];
            /**
             * Format: int64
             * @description The id of the article that this learningstep is associated with
             */
            articleId?: number;
            /** @description Determines if the title of the step should be displayed in viewmode */
            showTitle: boolean;
            /** @description The type of the step */
            type: string;
            /** @description Describes the copyright information for the learningstep */
            license?: components["schemas"]["LicenseDTO"];
            /** @description The full url to where the complete metainformation about the learningstep can be found */
            metaUrl: string;
            /** @description True if authenticated user may edit this learningstep */
            canEdit: boolean;
            /** @description The status of the learningstep */
            status: string;
            /** @description The supported languages of the learningstep */
            supportedLanguages: string[];
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
        /**
         * MessageDTO
         * @description Message set by administrator. Visible if administrator or owner of LearningPath
         */
        MessageDTO: {
            /** @description Message left on a learningpath by administrator */
            message: string;
            /** @description When the message was left */
            date: string;
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
         * NewCopyLearningPathV2DTO
         * @description Meta information for a new learningpath based on a copy
         */
        NewCopyLearningPathV2DTO: {
            /** @description The titles of the learningpath */
            title: string;
            /** @description The descriptions of the learningpath */
            description?: string;
            /** @description The chosen language */
            language: string;
            /** @description Url to cover-photo in NDLA image-api. */
            coverPhotoMetaUrl?: string;
            /**
             * Format: int32
             * @description The duration of the learningpath in minutes. Must be greater than 0
             */
            duration?: number;
            /** @description Searchable tags for the learningpath */
            tags?: string[];
            /** @description Describes the copyright information for the learningpath */
            copyright?: components["schemas"]["CopyrightDTO"];
        };
        /**
         * NewLearningPathV2DTO
         * @description Meta information for a new learningpath
         */
        NewLearningPathV2DTO: {
            /** @description The titles of the learningpath */
            title: string;
            /** @description The descriptions of the learningpath */
            description?: string;
            /** @description Url to cover-photo in NDLA image-api. */
            coverPhotoMetaUrl?: string;
            /**
             * Format: int32
             * @description The duration of the learningpath in minutes. Must be greater than 0
             */
            duration?: number;
            /** @description Searchable tags for the learningpath */
            tags?: string[];
            /** @description The chosen language */
            language: string;
            /** @description Describes the copyright information for the learningpath */
            copyright?: components["schemas"]["CopyrightDTO"];
            /** @description NDLA ID representing the editor responsible for this learningpath */
            responsibleId?: string;
            /** @description Information about comments attached to the learningpath */
            comments?: components["schemas"]["NewCommentDTO"][];
            /** @description A list of all revisions of the learningpath */
            revisionMeta?: components["schemas"]["RevisionMetaDTO"][];
            priority?: components["schemas"]["Priority"];
        };
        /**
         * NewLearningStepV2DTO
         * @description Information about a new learningstep
         */
        NewLearningStepV2DTO: {
            /** @description The titles of the learningstep */
            title: string;
            /** @description The introduction of the learningstep */
            introduction?: string;
            /** @description The descriptions of the learningstep */
            description?: string;
            /** @description The chosen language */
            language: string;
            /**
             * Format: int64
             * @description The article id this learningstep points to
             */
            articleId?: number;
            embedUrl?: components["schemas"]["EmbedUrlV2DTO"];
            /**
             * @description Determines if the title of the step should be displayed in viewmode.
             * @default false
             */
            showTitle?: boolean;
            /** @description The type of the step */
            type: string;
            /** @description Describes the copyright information for the learningstep */
            license?: string;
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
         * Priority
         * @description If the learningpath should be prioritized. Possible values are prioritized, on-hold, unspecified
         * @enum {string}
         */
        Priority: "prioritized" | "on-hold" | "unspecified";
        /**
         * ResponsibleDTO
         * @description Object with data representing the editor responsible for this learningpath
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
         * SearchParamsDTO
         * @description The search parameters
         */
        SearchParamsDTO: {
            /** @description The search query */
            query?: string;
            /** @description The ISO 639-1 language code describing language used in query-params */
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
            /** @description Return only learning paths that have one of the provided ids */
            ids?: number[];
            /** @description Return only learning paths that are tagged with this exact tag. */
            tag?: string;
            sort?: components["schemas"]["Sort"];
            /** @description Return all matched learning paths whether they exist on selected language or not. */
            fallback?: boolean;
            /** @description Return only learning paths that have the provided verification status. */
            verificationStatus?: string;
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
        };
        /**
         * SearchResultV2DTO
         * @description Information about search-results
         */
        SearchResultV2DTO: {
            /**
             * Format: int64
             * @description The total number of learningpaths matching this query
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
            results: components["schemas"]["LearningPathSummaryV2DTO"][];
        };
        /**
         * Sort
         * @description The sorting used on results. Default is by -relevance.
         * @enum {string}
         */
        Sort: "-id" | "id" | "-relevance" | "relevance" | "-lastUpdated" | "lastUpdated" | "-duration" | "duration" | "-title" | "title";
        /** TitleDTO */
        TitleDTO: {
            /** @description The title of the content. Must be plain text */
            title: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
        };
        /**
         * UpdateLearningPathStatusDTO
         * @description Status information about a learningpath
         */
        UpdateLearningPathStatusDTO: {
            /** @description The publishing status of the learningpath */
            status: string;
            /** @description Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath */
            message?: string;
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
         * UpdatedLearningPathV2DTO
         * @description Meta information for a new learningpath
         */
        UpdatedLearningPathV2DTO: {
            /**
             * Format: int32
             * @description The revision number for this learningpath
             */
            revision: number;
            /** @description The title of the learningpath */
            title?: string;
            /** @description The chosen language */
            language: string;
            /** @description The description of the learningpath */
            description?: string;
            /** @description Url to cover-photo in NDLA image-api. */
            coverPhotoMetaUrl?: string | null;
            /**
             * Format: int32
             * @description The duration of the learningpath in minutes. Must be greater than 0
             */
            duration?: number;
            /** @description Searchable tags for the learningpath */
            tags?: string[];
            /** @description Describes the copyright information for the learningpath */
            copyright?: components["schemas"]["CopyrightDTO"];
            /** @description Whether to delete a message connected to a learningpath by an administrator. */
            deleteMessage?: boolean;
            /** @description NDLA ID representing the editor responsible for this learningpath */
            responsibleId?: string | null;
            /** @description Information about comments attached to the learningpath */
            comments?: components["schemas"]["UpdatedCommentDTO"][];
            priority?: components["schemas"]["Priority"];
            /** @description A list of all revisions of the learningpath */
            revisionMeta?: components["schemas"]["RevisionMetaDTO"][];
        };
        /**
         * UpdatedLearningStepV2DTO
         * @description Information about a new learningstep
         */
        UpdatedLearningStepV2DTO: {
            /**
             * Format: int32
             * @description The revision number for this learningstep
             */
            revision: number;
            /** @description The title of the learningstep */
            title?: string;
            /** @description The introduction of the learningstep */
            introduction?: string | null;
            /** @description The chosen language */
            language: string;
            /** @description The description of the learningstep */
            description?: string | null;
            /** @description The embed content for the learningstep */
            embedUrl?: components["schemas"]["EmbedUrlV2DTO"] | null;
            /**
             * Format: int64
             * @description The article id this learningstep points to
             */
            articleId?: number | null;
            /** @description Determines if the title of the step should be displayed in viewmode */
            showTitle?: boolean;
            /** @description The type of the step */
            type?: string;
            /** @description Describes the copyright information for the learningstep */
            license?: string;
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
    "getLearningpath-apiV2Learningpaths": {
        parameters: {
            query?: {
                /** @description Return only Learningpaths with content matching the specified query. */
                query?: string;
                /** @description Return only Learningpaths that are tagged with this exact tag. */
                tag?: string;
                /** @description Return only Learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
                /** @description The sorting used on results.
                 *                  The following are supported: -id, id, -relevance, relevance, -lastUpdated, lastUpdated, -duration, duration, -title, title.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description Return only learning paths that have this verification status. */
                verificationStatus?: string;
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
    "postLearningpath-apiV2Learningpaths": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewLearningPathV2DTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
    "postLearningpath-apiV2LearningpathsSearch": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
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
    "getLearningpath-apiV2LearningpathsTags": {
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
                    "application/json": components["schemas"]["LearningPathTagsSummaryDTO"];
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
    "getLearningpath-apiV2LearningpathsLicenses": {
        parameters: {
            query?: {
                /** @description Query for filtering licenses. Only licenses containing filter-string are returned. */
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
    "getLearningpath-apiV2LearningpathsMine": {
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
                    "application/json": components["schemas"]["LearningPathV2DTO"][];
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
    "getLearningpath-apiV2LearningpathsContributors": {
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
                    "application/json": components["schemas"]["AuthorDTO"][];
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
    "getLearningpath-apiV2LearningpathsIds": {
        parameters: {
            query?: {
                /** @description Return only learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,). */
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
                    "application/json": components["schemas"]["LearningPathV2DTO"][];
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
    "getLearningpath-apiV2LearningpathsLearningpath_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
    "deleteLearningpath-apiV2LearningpathsLearningpath_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "patchLearningpath-apiV2LearningpathsLearningpath_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedLearningPathV2DTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
        };
    };
    "getLearningpath-apiV2LearningpathsLearningpath_idStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["LearningPathStatusDTO"];
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
    "putLearningpath-apiV2LearningpathsLearningpath_idStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateLearningPathStatusDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsTrash": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["LearningStepContainerSummaryDTO"];
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
    "getLearningpath-apiV2LearningpathsLearningpath_idLearningsteps": {
        parameters: {
            query?: {
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["LearningStepContainerSummaryDTO"];
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
    "postLearningpath-apiV2LearningpathsLearningpath_idLearningsteps": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewLearningStepV2DTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningStepV2DTO"];
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
    "getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
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
                    "application/json": components["schemas"]["LearningStepV2DTO"];
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
    "deleteLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "patchLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedLearningStepV2DTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningStepV2DTO"];
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
    "getLearningpath-apiV2LearningpathsContains-articleArticle_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the article to search with */
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
                    "application/json": components["schemas"]["LearningPathSummaryV2DTO"][];
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
    "getLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idStatus": {
        parameters: {
            query?: {
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
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
                    "application/json": components["schemas"]["LearningStepStatusDTO"];
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
    "putLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LearningStepStatusDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningStepV2DTO"];
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
    "postLearningpath-apiV2LearningpathsLearningpath_idCopy": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewCopyLearningPathV2DTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
    "putLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idSeqno": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LearningStepSeqNoDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LearningStepSeqNoDTO"];
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
    "deleteLearningpath-apiV2LearningpathsLearningpath_idLearningstepsLearningstep_idLanguageP1": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description Id of the learningstep. */
                learningstep_id: number;
                /** @description The ISO 639-1 language describing language. */
                p1: string;
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
                    "application/json": components["schemas"]["LearningStepV2DTO"];
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
    "getLearningpath-apiV2LearningpathsStatusStatus": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Status of LearningPaths */
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
                    "application/json": components["schemas"]["LearningPathV2DTO"][];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "deleteLearningpath-apiV2LearningpathsLearningpath_idLanguageP1": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
                /** @description The ISO 639-1 language describing language. */
                p1: string;
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
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "postLearningpath-apiV2LearningpathsLearningpath_idUpdate-taxonomy": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description Create taxonomy resource if missing for learningPath */
                "create-if-missing"?: boolean;
            };
            header?: never;
            path: {
                /** @description Id of the learningpath. */
                learningpath_id: number;
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
                    "application/json": components["schemas"]["LearningPathV2DTO"];
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
                    "application/json": components["schemas"]["AllErrors"] | components["schemas"]["ErrorBody"];
                };
            };
        };
    };
    "getLearningpath-apiV1Stats": {
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
                content?: never;
            };
            301: {
                headers: {
                    Location: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}
