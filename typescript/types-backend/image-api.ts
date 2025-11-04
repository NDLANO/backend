export type paths = {
    "/image-api/v2/images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find images.
         * @deprecated
         * @description Find images in the ndla.no database.
         */
        get: operations["getImage-apiV2Images"];
        put?: never;
        /**
         * Upload a new image with meta information.
         * @deprecated
         * @description Upload a new image file with meta data.
         */
        post: operations["postImage-apiV2Images"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v2/images/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used tags in images
         * @deprecated
         * @description Retrieves a list of all previously used tags in images
         */
        get: operations["getImage-apiV2ImagesTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v2/images/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find images.
         * @deprecated
         * @description Search for images in the ndla.no database.
         */
        post: operations["postImage-apiV2ImagesSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v2/images/{image_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for image.
         * @deprecated
         * @description Shows info of the image with submitted id.
         */
        get: operations["getImage-apiV2ImagesImage_id"];
        put?: never;
        post?: never;
        /**
         * Deletes the specified images meta data and file
         * @deprecated
         * @description Deletes the specified images meta data and file
         */
        delete: operations["deleteImage-apiV2ImagesImage_id"];
        options?: never;
        head?: never;
        /**
         * Update an existing image with meta information.
         * @deprecated
         * @description Updates an existing image with meta data.
         */
        patch: operations["patchImage-apiV2ImagesImage_id"];
        trace?: never;
    };
    "/image-api/v2/images/external_id/{external_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for image by external id.
         * @deprecated
         * @description Shows info of the image with submitted external id.
         */
        get: operations["getImage-apiV2ImagesExternal_idExternal_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v2/images/{image_id}/language/{language}": {
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
         * Delete language version of image metadata.
         * @deprecated
         * @description Delete language version of image metadata.
         */
        delete: operations["deleteImage-apiV2ImagesImage_idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find images.
         * @description Find images in the ndla.no database.
         */
        get: operations["getImage-apiV3Images"];
        put?: never;
        /**
         * Upload a new image with meta information.
         * @description Upload a new image file with meta data.
         */
        post: operations["postImage-apiV3Images"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch images that matches ids parameter.
         * @description Fetch images that matches ids parameter.
         */
        get: operations["getImage-apiV3ImagesIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used tags in images
         * @description Retrieves a list of all previously used tags in images
         */
        get: operations["getImage-apiV3ImagesTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find images.
         * @description Search for images in the ndla.no database.
         */
        post: operations["postImage-apiV3ImagesSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/{image_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for image.
         * @description Shows info of the image with submitted id.
         */
        get: operations["getImage-apiV3ImagesImage_id"];
        put?: never;
        post?: never;
        /**
         * Deletes the specified images meta data and file
         * @description Deletes the specified images meta data and file
         */
        delete: operations["deleteImage-apiV3ImagesImage_id"];
        options?: never;
        head?: never;
        /**
         * Update an existing image with meta information.
         * @description Updates an existing image with meta data.
         */
        patch: operations["patchImage-apiV3ImagesImage_id"];
        trace?: never;
    };
    "/image-api/v3/images/external_id/{external_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for image by external id.
         * @description Shows info of the image with submitted external id.
         */
        get: operations["getImage-apiV3ImagesExternal_idExternal_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/{image_id}/language/{language}": {
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
         * Delete language version of image metadata.
         * @description Delete language version of image metadata.
         */
        delete: operations["deleteImage-apiV3ImagesImage_idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/v3/images/{image_id}/copy": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Copy image meta data with a new image file
         * @description Copy image meta data with a new image file
         */
        post: operations["postImage-apiV3ImagesImage_idCopy"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/raw/id/{image_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch an image with options to resize and crop
         * @description Fetches a image with options to resize and crop
         */
        get: operations["getImage-apiRawIdImage_id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/image-api/raw/{image_name}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch an image with options to resize and crop
         * @description Fetches a image with options to resize and crop
         */
        get: operations["getImage-apiRawImage_name"];
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
         * ContributorType
         * @description The description of the author. Eg. Photographer or Supplier
         * @enum {string}
         */
        ContributorType: "artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer";
        /** CopyMetaDataAndFileForm */
        CopyMetaDataAndFileForm: {
            /** Format: binary */
            file: Blob;
        };
        /**
         * CopyrightDTO
         * @description Describes the copyright information for the image
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
         * EditorNoteDTO
         * @description Note about a change that happened to the image
         */
        EditorNoteDTO: {
            /** @description Timestamp of the change */
            timestamp: string;
            /** @description Who triggered the change */
            updatedBy: string;
            /** @description Editorial note */
            note: string;
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
        /** ImageAltTextDTO */
        ImageAltTextDTO: {
            /** @description The alternative text for the image */
            alttext: string;
            /** @description ISO 639-1 code that represents the language used in the alternative text */
            language: string;
        };
        /** ImageCaptionDTO */
        ImageCaptionDTO: {
            /** @description The caption for the image */
            caption: string;
            /** @description ISO 639-1 code that represents the language used in the caption */
            language: string;
        };
        /**
         * ImageDimensionsDTO
         * @description Dimensions of the image
         */
        ImageDimensionsDTO: {
            /**
             * Format: int32
             * @description The width of the image in pixels
             */
            width: number;
            /**
             * Format: int32
             * @description The height of the image in pixels
             */
            height: number;
        };
        /**
         * ImageFileDTO
         * @description Describes the image file
         */
        ImageFileDTO: {
            /** @description File name pointing to image file */
            fileName: string;
            /**
             * Format: int64
             * @description The size of the image in bytes
             */
            size: number;
            /** @description The mimetype of the image */
            contentType: string;
            /** @description The full url to where the image can be downloaded */
            imageUrl: string;
            dimensions?: components["schemas"]["ImageDimensionsDTO"];
            /** @description ISO 639-1 code that represents the language used in the caption */
            language: string;
        };
        /**
         * ImageMetaInformationV2DTO
         * @description Meta information for the image
         */
        ImageMetaInformationV2DTO: {
            /** @description The unique id of the image */
            id: string;
            /** @description The url to where this information can be found */
            metaUrl: string;
            /** @description The title for the image */
            title: components["schemas"]["ImageTitleDTO"];
            /** @description Alternative text for the image */
            alttext: components["schemas"]["ImageAltTextDTO"];
            /** @description The full url to where the image can be downloaded */
            imageUrl: string;
            /**
             * Format: int64
             * @description The size of the image in bytes
             */
            size: number;
            /** @description The mimetype of the image */
            contentType: string;
            copyright: components["schemas"]["CopyrightDTO"];
            tags: components["schemas"]["ImageTagDTO"];
            /** @description Searchable caption for the image */
            caption: components["schemas"]["ImageCaptionDTO"];
            /** @description Supported languages for the image title, alt-text, tags and caption. */
            supportedLanguages: string[];
            /** @description Describes when the image was created */
            created: string;
            /** @description Describes who created the image */
            createdBy: string;
            /** @description Describes if the model has released use of the image */
            modelRelease: string;
            /** @description Describes the changes made to the image, only visible to editors */
            editorNotes?: components["schemas"]["EditorNoteDTO"][];
            imageDimensions?: components["schemas"]["ImageDimensionsDTO"];
        };
        /**
         * ImageMetaInformationV3DTO
         * @description Meta information for the image
         */
        ImageMetaInformationV3DTO: {
            /** @description The unique id of the image */
            id: string;
            /** @description The url to where this information can be found */
            metaUrl: string;
            /** @description The title for the image */
            title: components["schemas"]["ImageTitleDTO"];
            /** @description Alternative text for the image */
            alttext: components["schemas"]["ImageAltTextDTO"];
            copyright: components["schemas"]["CopyrightDTO"];
            tags: components["schemas"]["ImageTagDTO"];
            /** @description Searchable caption for the image */
            caption: components["schemas"]["ImageCaptionDTO"];
            /** @description Supported languages for the image title, alt-text, tags and caption. */
            supportedLanguages: string[];
            /** @description Describes when the image was created */
            created: string;
            /** @description Describes who created the image */
            createdBy: string;
            /** @description Describes if the model has released use of the image */
            modelRelease: string;
            /** @description Describes the changes made to the image, only visible to editors */
            editorNotes?: components["schemas"]["EditorNoteDTO"][];
            image: components["schemas"]["ImageFileDTO"];
        };
        /**
         * ImageMetaSummaryDTO
         * @description Summary of meta information for an image
         */
        ImageMetaSummaryDTO: {
            /** @description The unique id of the image */
            id: string;
            /** @description The title for this image */
            title: components["schemas"]["ImageTitleDTO"];
            /** @description The copyright authors for this image */
            contributors: string[];
            /** @description The alt text for this image */
            altText: components["schemas"]["ImageAltTextDTO"];
            /** @description The caption for this image */
            caption: components["schemas"]["ImageCaptionDTO"];
            /** @description The full url to where a preview of the image can be downloaded */
            previewUrl: string;
            /** @description The full url to where the complete metainformation about the image can be found */
            metaUrl: string;
            /** @description Describes the license of the image */
            license: string;
            /** @description List of supported languages in priority */
            supportedLanguages: string[];
            /** @description Describes if the model has released use of the image */
            modelRelease?: string;
            /** @description Describes the changes made to the image, only visible to editors */
            editorNotes?: string[];
            /** @description The time and date of last update */
            lastUpdated: string;
            /**
             * Format: int64
             * @description The size of the image in bytes
             */
            fileSize: number;
            /** @description The mimetype of the image */
            contentType: string;
            imageDimensions?: components["schemas"]["ImageDimensionsDTO"];
        };
        /**
         * ImageTagDTO
         * @description Searchable tags for the image
         */
        ImageTagDTO: {
            /** @description The searchable tag. */
            tags: string[];
            /** @description ISO 639-1 code that represents the language used in tag */
            language: string;
        };
        /** ImageTitleDTO */
        ImageTitleDTO: {
            /** @description The freetext title of the image */
            title: string;
            /** @description ISO 639-1 code that represents the language used in title */
            language: string;
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
        /** MetaDataAndFileForm */
        MetaDataAndFileForm: {
            metadata: components["schemas"]["NewImageMetaInformationV2DTO"];
            /** Format: binary */
            file: Blob;
        };
        /**
         * NewImageMetaInformationV2DTO
         * @description Meta information for the image
         */
        NewImageMetaInformationV2DTO: {
            /** @description Title for the image */
            title: string;
            /** @description Alternative text for the image */
            alttext?: string;
            copyright: components["schemas"]["CopyrightDTO"];
            /** @description Searchable tags for the image */
            tags: string[];
            /** @description Caption for the image */
            caption: string;
            /** @description ISO 639-1 code that represents the language used in the caption */
            language: string;
            /** @description Describes if the model has released use of the image, allowed values are 'not-set', 'yes', 'no', and 'not-applicable', defaults to 'no' */
            modelReleased?: string;
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
         * SearchParamsDTO
         * @description The search parameters
         */
        SearchParamsDTO: {
            /** @description Return only images with titles, alt-texts or tags matching the specified query. */
            query?: string;
            /** @description Return only images with provided license. */
            license?: string;
            /** @description The ISO 639-1 language code describing language used in query-params */
            language?: string;
            /** @description Fallback to existing language if language is specified. */
            fallback?: boolean;
            /**
             * Format: int32
             * @description Return only images with full size larger than submitted value in bytes.
             */
            minimumSize?: number;
            /**
             * @deprecated
             * @description Return copyrighted images. May be omitted.
             */
            includeCopyrighted?: boolean;
            sort?: components["schemas"]["Sort"];
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
            /** @description Only show podcast friendly images. */
            podcastFriendly?: boolean;
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description Return only images with one of the provided values for modelReleased. */
            modelReleased?: string[];
            /** @description Filter editors of the image(s). Multiple values can be specified in a comma separated list. */
            users?: string[];
        };
        /**
         * SearchResultDTO
         * @description Information about search-results
         */
        SearchResultDTO: {
            /**
             * Format: int64
             * @description The total number of images matching this query
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
            results: components["schemas"]["ImageMetaSummaryDTO"][];
        };
        /**
         * SearchResultV3DTO
         * @description Information about search-results
         */
        SearchResultV3DTO: {
            /**
             * Format: int64
             * @description The total number of images matching this query
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
            results: components["schemas"]["ImageMetaInformationV3DTO"][];
        };
        /**
         * Sort
         * @description The sorting used on results. The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id. Default is by -relevance (desc) when query is set, and title (asc) when query is empty.
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
         * UpdateImageMetaInformationDTO
         * @description Meta information for the image
         */
        UpdateImageMetaInformationDTO: {
            /** @description ISO 639-1 code that represents the language */
            language: string;
            /** @description Title for the image */
            title?: string;
            /** @description Alternative text for the image */
            alttext?: string | null;
            copyright?: components["schemas"]["CopyrightDTO"];
            /** @description Searchable tags for the image */
            tags?: string[];
            /** @description Caption for the image */
            caption?: string;
            /** @description Describes if the model has released use of the image */
            modelReleased?: string;
        };
        /** UpdateMetaDataAndFileForm */
        UpdateMetaDataAndFileForm: {
            metadata: components["schemas"]["UpdateImageMetaInformationDTO"];
            /** Format: binary */
            file?: Blob;
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
export type AllErrors = components['schemas']['AllErrors'];
export type AuthorDto = components['schemas']['AuthorDTO'];
export type ContributorType = components['schemas']['ContributorType'];
export type CopyMetaDataAndFileForm = components['schemas']['CopyMetaDataAndFileForm'];
export type CopyrightDto = components['schemas']['CopyrightDTO'];
export type EditorNoteDto = components['schemas']['EditorNoteDTO'];
export type ErrorBody = components['schemas']['ErrorBody'];
export type ImageAltTextDto = components['schemas']['ImageAltTextDTO'];
export type ImageCaptionDto = components['schemas']['ImageCaptionDTO'];
export type ImageDimensionsDto = components['schemas']['ImageDimensionsDTO'];
export type ImageFileDto = components['schemas']['ImageFileDTO'];
export type ImageMetaInformationV2Dto = components['schemas']['ImageMetaInformationV2DTO'];
export type ImageMetaInformationV3Dto = components['schemas']['ImageMetaInformationV3DTO'];
export type ImageMetaSummaryDto = components['schemas']['ImageMetaSummaryDTO'];
export type ImageTagDto = components['schemas']['ImageTagDTO'];
export type ImageTitleDto = components['schemas']['ImageTitleDTO'];
export type LicenseDto = components['schemas']['LicenseDTO'];
export type MetaDataAndFileForm = components['schemas']['MetaDataAndFileForm'];
export type NewImageMetaInformationV2Dto = components['schemas']['NewImageMetaInformationV2DTO'];
export type NotFoundWithSupportedLanguages = components['schemas']['NotFoundWithSupportedLanguages'];
export type SearchParamsDto = components['schemas']['SearchParamsDTO'];
export type SearchResultDto = components['schemas']['SearchResultDTO'];
export type SearchResultV3Dto = components['schemas']['SearchResultV3DTO'];
export type Sort = components['schemas']['Sort'];
export type TagsSearchResultDto = components['schemas']['TagsSearchResultDTO'];
export type UpdateImageMetaInformationDto = components['schemas']['UpdateImageMetaInformationDTO'];
export type UpdateMetaDataAndFileForm = components['schemas']['UpdateMetaDataAndFileForm'];
export type ValidationErrorBody = components['schemas']['ValidationErrorBody'];
export type ValidationMessage = components['schemas']['ValidationMessage'];
export type $defs = Record<string, never>;
export interface operations {
    "getImage-apiV2Images": {
        parameters: {
            query?: {
                /** @description Return only images with titles, alt-texts or tags matching the specified query. */
                query?: string;
                /** @description Return only images with full size larger than submitted value in bytes. */
                "minimum-size"?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description Return only images with provided license. */
                license?: string;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description Filter images that are podcast friendly. Width==heigth and between 1400 and 3000. */
                "podcast-friendly"?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: yes,no,not-applicable,not-set */
                "model-released"?: string[];
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
                    "application/json": components["schemas"]["SearchResultDTO"];
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
    "postImage-apiV2Images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["MetaDataAndFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ImageMetaInformationV2DTO"];
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
            413: {
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
    "getImage-apiV2ImagesTag-search": {
        parameters: {
            query?: {
                /** @description Return only images with titles, alt-texts or tags matching the specified query. */
                query?: string;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
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
    "postImage-apiV2ImagesSearch": {
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
                    "application/json": components["schemas"]["SearchResultDTO"];
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
    "getImage-apiV2ImagesImage_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
                    "application/json": components["schemas"]["ImageMetaInformationV2DTO"];
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
    "deleteImage-apiV2ImagesImage_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
    "patchImage-apiV2ImagesImage_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["UpdateMetaDataAndFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ImageMetaInformationV2DTO"];
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
    "getImage-apiV2ImagesExternal_idExternal_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description External node id of the image that needs to be fetched. */
                external_id: string;
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
                    "application/json": components["schemas"]["ImageMetaInformationV2DTO"];
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
    "deleteImage-apiV2ImagesImage_idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
                    "application/json": components["schemas"]["ImageMetaInformationV2DTO"];
                };
            };
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
    "getImage-apiV3Images": {
        parameters: {
            query?: {
                /** @description Return only images with titles, alt-texts or tags matching the specified query. */
                query?: string;
                /** @description Return only images with full size larger than submitted value in bytes. */
                "minimum-size"?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
                /** @description Return only images with provided license. */
                license?: string;
                /**
                 * @deprecated
                 * @description Return copyrighted images. May be omitted.
                 */
                includeCopyrighted?: boolean;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description Filter images that are podcast friendly. Width==heigth and between 1400 and 3000. */
                "podcast-friendly"?: boolean;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: yes,no,not-applicable,not-set */
                "model-released"?: string[];
                /** @description List of users to filter by.
                 *     The value to search for is the user-id from Auth0.
                 *     UpdatedBy on article and user in editorial-notes are searched. */
                users?: string[];
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
                    "application/json": components["schemas"]["SearchResultV3DTO"];
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
    "postImage-apiV3Images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["MetaDataAndFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
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
    "getImage-apiV3ImagesIds": {
        parameters: {
            query?: {
                /** @description Return only images that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
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
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"][];
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
    "getImage-apiV3ImagesTag-search": {
        parameters: {
            query?: {
                /** @description Return only images with titles, alt-texts or tags matching the specified query. */
                query?: string;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
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
    "postImage-apiV3ImagesSearch": {
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
                    "application/json": components["schemas"]["SearchResultV3DTO"];
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
    "getImage-apiV3ImagesImage_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
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
    "deleteImage-apiV3ImagesImage_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
    "patchImage-apiV3ImagesImage_id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["UpdateMetaDataAndFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
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
    "getImage-apiV3ImagesExternal_idExternal_id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description External node id of the image that needs to be fetched. */
                external_id: string;
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
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
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
    "deleteImage-apiV3ImagesImage_idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
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
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
                };
            };
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
    "postImage-apiV3ImagesImage_idCopy": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Image_id of the image that needs to be fetched. */
                image_id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["CopyMetaDataAndFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ImageMetaInformationV3DTO"];
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
    "getImage-apiRawIdImage_id": {
        parameters: {
            query?: {
                /** @description The target width to resize the image (the unit is pixles). Image proportions are kept intact */
                width?: number;
                /** @description The target height to resize the image (the unit is pixles). Image proportions are kept intact */
                height?: number;
                /** @description The first image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied */
                cropStartX?: number;
                /** @description The first image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied */
                cropStartY?: number;
                /** @description The end image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied */
                cropEndX?: number;
                /** @description The end image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied */
                cropEndY?: number;
                /** @description The unit of the crop parameters. Can be either 'percent' or 'pixel'. If omitted the unit is assumed to be 'percent' */
                cropUnit?: string;
                /** @description The end image coordinate X, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied */
                focalX?: number;
                /** @description The end image coordinate Y, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied */
                focalY?: number;
                /** @description The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead. */
                ratio?: number;
                /** @description The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead. */
                language?: string;
            };
            header?: {
                /** @description Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access. */
                "app-key"?: string;
            };
            path: {
                /** @description The ID of the image */
                image_id: number;
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
                    "application/octet-stream": Blob;
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
    "getImage-apiRawImage_name": {
        parameters: {
            query?: {
                /** @description The target width to resize the image (the unit is pixles). Image proportions are kept intact */
                width?: number;
                /** @description The target height to resize the image (the unit is pixles). Image proportions are kept intact */
                height?: number;
                /** @description The first image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied */
                cropStartX?: number;
                /** @description The first image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied */
                cropStartY?: number;
                /** @description The end image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied */
                cropEndX?: number;
                /** @description The end image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied */
                cropEndY?: number;
                /** @description The unit of the crop parameters. Can be either 'percent' or 'pixel'. If omitted the unit is assumed to be 'percent' */
                cropUnit?: string;
                /** @description The end image coordinate X, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied */
                focalX?: number;
                /** @description The end image coordinate Y, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied */
                focalY?: number;
                /** @description The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead. */
                ratio?: number;
                /** @description The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead. */
                language?: string;
            };
            header?: {
                /** @description Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access. */
                "app-key"?: string;
            };
            path: {
                /** @description The name of the image */
                image_name: string;
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
                    "application/octet-stream": Blob;
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
