export type paths = {
    "/audio-api/v1/audio": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find audio files
         * @description Shows all the audio files in the ndla.no database. You can search it too.
         */
        get: operations["getAudio-apiV1Audio"];
        put?: never;
        /**
         * Upload a new audio file with meta information
         * @description Upload a new audio file with meta data
         */
        post: operations["postAudio-apiV1Audio"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/audio/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find audio files
         * @description Shows all the audio files in the ndla.no database. You can search it too.
         */
        post: operations["postAudio-apiV1AudioSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/audio/ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch audio that matches ids parameter.
         * @description Fetch audios that matches ids parameter.
         */
        get: operations["getAudio-apiV1AudioIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/audio/tag-search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Retrieves a list of all previously used tags in audios
         * @description Retrieves a list of all previously used tags in audios
         */
        get: operations["getAudio-apiV1AudioTag-search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/audio/{audio-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for audio file
         * @description Shows info of the audio with submitted id.
         */
        get: operations["getAudio-apiV1AudioAudio-id"];
        /**
         * Upload audio for a different language or update metadata for an existing audio-file
         * @description Update the metadata for an existing language, or upload metadata for a new language.
         */
        put: operations["putAudio-apiV1AudioAudio-id"];
        post?: never;
        /**
         * Deletes audio with the specified id
         * @description Deletes audio with the specified id
         */
        delete: operations["deleteAudio-apiV1AudioAudio-id"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/audio/{audio-id}/language/{language}": {
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
         * Delete language version of audio metadata.
         * @description Delete language version of audio metadata.
         */
        delete: operations["deleteAudio-apiV1AudioAudio-idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/series": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find series
         * @description Shows all the series. Also searchable.
         */
        get: operations["getAudio-apiV1Series"];
        put?: never;
        /**
         * Create a new series with meta information
         * @description Create a new series with meta information
         */
        post: operations["postAudio-apiV1Series"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/series/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Find series
         * @description Shows all the series. Also searchable.
         */
        post: operations["postAudio-apiV1SeriesSearch"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/series/{series-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch information for series
         * @description Shows info of the series with submitted id.
         */
        get: operations["getAudio-apiV1SeriesSeries-id"];
        /**
         * Upload audio for a different language or update metadata for an existing audio-file
         * @description Update the metadata for an existing language, or upload metadata for a new language.
         */
        put: operations["putAudio-apiV1SeriesSeries-id"];
        post?: never;
        /**
         * Deletes series with the specified id
         * @description Deletes series with the specified id
         */
        delete: operations["deleteAudio-apiV1SeriesSeries-id"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/series/{series-id}/language/{language}": {
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
         * Delete language version of audio metadata.
         * @description Delete language version of audio metadata.
         */
        delete: operations["deleteAudio-apiV1SeriesSeries-idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/transcription/{videoId}/{language}/extract-audio": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get audio extraction status
         * @description Get the status of the audio extraction from a Brightcove video.
         */
        get: operations["getAudio-apiV1TranscriptionVideoidLanguageExtract-audio"];
        put?: never;
        /**
         * Extract audio from video
         * @description Extracts audio from a Brightcove video and uploads it to S3.
         */
        post: operations["postAudio-apiV1TranscriptionVideoidLanguageExtract-audio"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/transcription/video/{videoId}/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the transcription status of a video
         * @description Get the transcription of a video.
         */
        get: operations["getAudio-apiV1TranscriptionVideoVideoidLanguage"];
        put?: never;
        /**
         * Transcribe video
         * @description Transcribes a video and uploads the transcription to S3.
         */
        post: operations["postAudio-apiV1TranscriptionVideoVideoidLanguage"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/transcription/audio/{audioName}/{audioId}/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Transcribe audio
         * @description Transcribes an audiofile and uploads the transcription to S3.
         */
        post: operations["postAudio-apiV1TranscriptionAudioAudionameAudioidLanguage"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/audio-api/v1/transcription/audio/{audioId}/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the transcription status of an audiofile
         * @description Get the transcription of an audiofile .
         */
        get: operations["getAudio-apiV1TranscriptionAudioAudioidLanguage"];
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
         * AudioDTO
         * @description The audio file for this language
         */
        AudioDTO: {
            /** @description The path to where the audio is located */
            url: string;
            /** @description The mime type of the audio file */
            mimeType: string;
            /**
             * Format: int64
             * @description The size of the audio file
             */
            fileSize: number;
            /** @description The current language for this audio */
            language: string;
        };
        /**
         * AudioMetaInformationDTO
         * @description Meta information about the audio object
         */
        AudioMetaInformationDTO: {
            /**
             * Format: int64
             * @description The unique id of this audio
             */
            id: number;
            /**
             * Format: int32
             * @description The revision number of this audio
             */
            revision: number;
            /** @description The title of the audio file */
            title: components["schemas"]["TitleDTO"];
            audioFile: components["schemas"]["AudioDTO"];
            copyright: components["schemas"]["CopyrightDTO"];
            tags: components["schemas"]["TagDTO"];
            /** @description The languages available for this audio */
            supportedLanguages: string[];
            /** @description Type of audio. 'standard', or 'podcast'. */
            audioType: string;
            podcastMeta?: components["schemas"]["PodcastMetaDTO"];
            /** @description Meta information about series if the audio is a podcast and a part of a series. */
            series?: components["schemas"]["SeriesDTO"];
            /** @description Manuscript for the audio */
            manuscript?: components["schemas"]["ManuscriptDTO"];
            /** @description The time of creation for the audio-file */
            created: string;
            /** @description The time of last update for the audio-file */
            updated: string;
        };
        /**
         * AudioSummaryDTO
         * @description Short summary of information about the audio
         */
        AudioSummaryDTO: {
            /**
             * Format: int64
             * @description The unique id of the audio
             */
            id: number;
            /** @description The title of the audio */
            title: components["schemas"]["TitleDTO"];
            /** @description The audioType. Possible values standard and podcast */
            audioType: string;
            /** @description The full url to where the complete information about the audio can be found */
            url: string;
            /** @description Describes the license of the audio */
            license: string;
            /** @description A list of available languages for this audio */
            supportedLanguages: string[];
            /** @description A manuscript for the audio */
            manuscript?: components["schemas"]["ManuscriptDTO"];
            podcastMeta?: components["schemas"]["PodcastMetaDTO"];
            /** @description Series that the audio is part of */
            series?: components["schemas"]["SeriesSummaryDTO"];
            /** @description The time and date of last update */
            lastUpdated: string;
        };
        /**
         * AudioSummarySearchResultDTO
         * @description Information about audio summary search-results
         */
        AudioSummarySearchResultDTO: {
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
            results: components["schemas"]["AudioSummaryDTO"][];
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
         * @description Copyright information for the audio files
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
        /** CoverPhotoDTO */
        CoverPhotoDTO: {
            /** @description Id for the coverPhoto in image-api */
            id: string;
            /** @description Url to the coverPhoto */
            url: string;
            /** @description Alttext for the coverPhoto */
            altText: string;
        };
        /**
         * DescriptionDTO
         * @description The description of the series
         */
        DescriptionDTO: {
            /** @description The description of the element */
            description: string;
            /** @description ISO 639-1 code that represents the language used in the description */
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
        /** ManuscriptDTO */
        ManuscriptDTO: {
            /** @description The manuscript of the audio file */
            manuscript: string;
            /** @description ISO 639-1 code that represents the language used in the manuscript */
            language: string;
        };
        /** MetaDataAndFileForm */
        MetaDataAndFileForm: {
            metadata: components["schemas"]["NewAudioMetaInformationDTO"];
            /** Format: binary */
            file: Blob;
        };
        /** MetaDataAndOptFileForm */
        MetaDataAndOptFileForm: {
            metadata: components["schemas"]["UpdatedAudioMetaInformationDTO"];
            /** Format: binary */
            file?: Blob;
        };
        /**
         * NewAudioMetaInformationDTO
         * @description Meta information about the audio object
         */
        NewAudioMetaInformationDTO: {
            /** @description The title of the audio file */
            title: string;
            /** @description ISO 639-1 code that represents the language used in this resource */
            language: string;
            copyright: components["schemas"]["CopyrightDTO"];
            /** @description Tags for this audio file */
            tags: string[];
            /** @description Type of audio. 'standard', or 'podcast', defaults to 'standard' */
            audioType?: string;
            podcastMeta?: components["schemas"]["NewPodcastMetaDTO"];
            /**
             * Format: int64
             * @description Id of series if the audio is a podcast and a part of a series.
             */
            seriesId?: number;
            /** @description Manuscript for the audio */
            manuscript?: string;
        };
        /**
         * NewPodcastMetaDTO
         * @description Meta information about podcast, only applicable if audioType is 'podcast'.
         */
        NewPodcastMetaDTO: {
            /** @description Introduction for the podcast */
            introduction: string;
            /** @description Cover photo for the podcast */
            coverPhotoId: string;
            /** @description Cover photo alttext for the podcast */
            coverPhotoAltText: string;
        };
        /**
         * NewSeriesDTO
         * @description Meta information about podcast series
         */
        NewSeriesDTO: {
            /** @description Header for the series */
            title: string;
            /** @description Description for the series */
            description: string;
            /** @description Cover photo for the series */
            coverPhotoId: string;
            /** @description Cover photo alttext for the series */
            coverPhotoAltText: string;
            /** @description Ids for episodes of the series */
            episodes: number[];
            /** @description ISO 639-1 code that represents the language used in this resource */
            language: string;
            /**
             * Format: int32
             * @description Revision number of this series (Only used to do locking when updating)
             */
            revision?: number;
            /** @description Specifies if this series generates rss-feed */
            hasRSS?: boolean;
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
         * PodcastMetaDTO
         * @description Meta information about podcast, only applicable if audioType is 'podcast'.
         */
        PodcastMetaDTO: {
            /** @description Introduction for the podcast */
            introduction: string;
            /** @description Cover photo for the podcast */
            coverPhoto: components["schemas"]["CoverPhotoDTO"];
            /** @description ISO 639-1 code that represents the language used in the title */
            language: string;
        };
        /**
         * SearchParamsDTO
         * @description The search parameters
         */
        SearchParamsDTO: {
            /** @description Return only audio with titles, alt-texts or tags matching the specified query. */
            query?: string;
            /** @description Return only audio with provided license. */
            license?: string;
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
            sort?: components["schemas"]["Sort"];
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description Type of audio to filter by. */
            audioType?: string;
            /** @description Filter result by whether they are a part of a series or not.
             *     'true' will return only audios that are a part of a series.
             *     'false' will return only audios that are NOT a part of a series.
             *     Not specifying will return both audios that are a part of a series and not. */
            filterBySeries?: boolean;
            /** @description Return all matched audios whether they exist on selected language or not. */
            fallback?: boolean;
        };
        /** SeriesDTO */
        SeriesDTO: {
            /**
             * Format: int64
             * @description The unique id of this series
             */
            id: number;
            /**
             * Format: int32
             * @description The revision number of this series
             */
            revision: number;
            /** @description The title of the series */
            title: components["schemas"]["TitleDTO"];
            description: components["schemas"]["DescriptionDTO"];
            /** @description Cover photo for the series */
            coverPhoto: components["schemas"]["CoverPhotoDTO"];
            /** @description The metainfo of the episodes in the series */
            episodes?: components["schemas"]["AudioMetaInformationDTO"][];
            /** @description A list of available languages for this series */
            supportedLanguages: string[];
            /** @description Specifies if this series generates rss-feed */
            hasRSS: boolean;
        };
        /**
         * SeriesSearchParamsDTO
         * @description The search parameters
         */
        SeriesSearchParamsDTO: {
            /** @description Return only series with titles, alt-texts or tags matching the specified query. */
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
            sort?: components["schemas"]["Sort"];
            /** @description A search context retrieved from the response header of a previous search. */
            scrollId?: string;
            /** @description Return all matched series whether they exist on selected language or not. */
            fallback?: boolean;
        };
        /** SeriesSummaryDTO */
        SeriesSummaryDTO: {
            /**
             * Format: int64
             * @description The unique id of the series
             */
            id: number;
            /** @description The title of the series */
            title: components["schemas"]["TitleDTO"];
            description: components["schemas"]["DescriptionDTO"];
            /** @description A list of available languages for this series */
            supportedLanguages: string[];
            /** @description A list of episode summaries */
            episodes?: components["schemas"]["AudioSummaryDTO"][];
            /** @description Cover photo for the series */
            coverPhoto: components["schemas"]["CoverPhotoDTO"];
        };
        /**
         * SeriesSummarySearchResultDTO
         * @description Information about series summary search-results
         */
        SeriesSummarySearchResultDTO: {
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
            results: components["schemas"]["SeriesSummaryDTO"][];
        };
        /**
         * Sort
         * @description The sorting used on results. Default is by -relevance.
         * @enum {string}
         */
        Sort: "-relevance" | "relevance" | "-title" | "title" | "-lastUpdated" | "lastUpdated" | "-id" | "id";
        /**
         * TagDTO
         * @description Tags for this audio file
         */
        TagDTO: {
            /** @description The searchable tag. */
            tags: string[];
            /** @description ISO 639-1 code that represents the language used in tag */
            language: string;
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
        /** TitleDTO */
        TitleDTO: {
            /** @description The title of the audio file */
            title: string;
            /** @description ISO 639-1 code that represents the language used in the title */
            language: string;
        };
        /**
         * TranscriptionResultDTO
         * @description The result of a transcription job
         */
        TranscriptionResultDTO: {
            /** @description The status of the transcription job */
            status: string;
            /** @description The transcription of the audio */
            transcription?: string;
        };
        /**
         * UpdatedAudioMetaInformationDTO
         * @description Meta information about the audio object
         */
        UpdatedAudioMetaInformationDTO: {
            /**
             * Format: int32
             * @description The revision number of this audio
             */
            revision: number;
            /** @description The title of the audio file */
            title: string;
            /** @description ISO 639-1 code that represents the language used in this resource */
            language: string;
            copyright: components["schemas"]["CopyrightDTO"];
            /** @description Tags for this audio file */
            tags: string[];
            /** @description Type of audio. 'standard', or 'podcast', defaults to 'standard' */
            audioType?: string;
            podcastMeta?: components["schemas"]["NewPodcastMetaDTO"];
            /**
             * Format: int64
             * @description Id of series if the audio is a podcast and a part of a series.
             */
            seriesId?: number;
            /** @description Manuscript for the audio */
            manuscript?: string;
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
    "getAudio-apiV1Audio": {
        parameters: {
            query?: {
                /** @description Return only results with titles or tags matching the specified query. */
                query?: string;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Return only audio with provided license. */
                license?: string;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
                /** @description Only return types of the specified value.
                 *     Possible values are 'podcast, standard' */
                "audio-type"?: string;
                /** @description Filter result by whether they are a part of a series or not.
                 *     'true' will return only audios that are a part of a series.
                 *     'false' will return only audios that are NOT a part of a series.
                 *     Not specifying will return both audios that are a part of a series and not. */
                "filter-by-series"?: boolean;
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
                    /**
                     * @description null
                     * @example null
                     */
                    "search-context"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AudioSummarySearchResultDTO"];
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
    "postAudio-apiV1Audio": {
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
                    "application/json": components["schemas"]["AudioMetaInformationDTO"];
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
    "postAudio-apiV1AudioSearch": {
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
                    /**
                     * @description null
                     * @example null
                     */
                    "search-context"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AudioSummarySearchResultDTO"];
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
    "getAudio-apiV1AudioIds": {
        parameters: {
            query?: {
                /** @description Return only audios that have one of the provided ids. To provide multiple ids, separate by comma (,). */
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
                    "application/json": components["schemas"]["AudioMetaInformationDTO"][];
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
    "getAudio-apiV1AudioTag-search": {
        parameters: {
            query?: {
                /** @description Return only results with titles or tags matching the specified query. */
                query?: string;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
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
    "getAudio-apiV1AudioAudio-id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Id of audio. */
                "audio-id": number;
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
                    "application/json": components["schemas"]["AudioMetaInformationDTO"];
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
    "putAudio-apiV1AudioAudio-id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of audio. */
                "audio-id": number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": components["schemas"]["MetaDataAndOptFileForm"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AudioMetaInformationDTO"];
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
    "deleteAudio-apiV1AudioAudio-id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of audio. */
                "audio-id": number;
            };
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
    "deleteAudio-apiV1AudioAudio-idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of audio. */
                "audio-id": number;
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
                    "application/json": components["schemas"]["AudioMetaInformationDTO"];
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
    "getAudio-apiV1Series": {
        parameters: {
            query?: {
                /** @description Return only results with titles or tags matching the specified query. */
                query?: string;
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description The sorting used on results.
                 *                  The following are supported: -relevance, relevance, -title, title, -lastUpdated, lastUpdated, -id, id.
                 *                  Default is by -relevance (desc) when query is set, and title (asc) when query is empty. */
                sort?: string;
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. Defaults to 10 and max is 10000. */
                "page-size"?: number;
                /** @description A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: [0,initial,start,first].
                 *     When scrolling, the parameters from the initial search is used, except in the case of 'language'.
                 *     This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after 1m).
                 *     If you are not paginating past 10000 hits, you can ignore this and use 'page' and 'page-size' instead.
                 *      */
                "search-context"?: string;
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
                    /**
                     * @description null
                     * @example null
                     */
                    "search-context"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SeriesSummarySearchResultDTO"];
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
    "postAudio-apiV1Series": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewSeriesDTO"];
            };
        };
        responses: {
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SeriesDTO"];
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
    "postAudio-apiV1SeriesSearch": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SeriesSearchParamsDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    /**
                     * @description null
                     * @example null
                     */
                    "search-context"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SeriesSummarySearchResultDTO"];
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
    "getAudio-apiV1SeriesSeries-id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
            };
            header?: never;
            path: {
                /** @description Id of series. */
                "series-id": number;
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
                    "application/json": components["schemas"]["SeriesDTO"];
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
    "putAudio-apiV1SeriesSeries-id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of series. */
                "series-id": number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewSeriesDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SeriesDTO"];
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
    "deleteAudio-apiV1SeriesSeries-id": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of series. */
                "series-id": number;
            };
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
    "deleteAudio-apiV1SeriesSeries-idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description Id of series. */
                "series-id": number;
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
                    "application/json": components["schemas"]["SeriesDTO"];
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
    "getAudio-apiV1TranscriptionVideoidLanguageExtract-audio": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The video id to transcribe */
                videoId: string;
                /** @description The language to run the transcription in */
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
    "postAudio-apiV1TranscriptionVideoidLanguageExtract-audio": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The video id to transcribe */
                videoId: string;
                /** @description The language to run the transcription in */
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
    "getAudio-apiV1TranscriptionVideoVideoidLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The video id to transcribe */
                videoId: string;
                /** @description The language to run the transcription in */
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
                    "application/json": components["schemas"]["TranscriptionResultDTO"];
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
            405: {
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
    "postAudio-apiV1TranscriptionVideoVideoidLanguage": {
        parameters: {
            query?: {
                /** @description The maximum number of speakers in the video */
                maxSpeaker?: number;
            };
            header?: never;
            path: {
                /** @description The video id to transcribe */
                videoId: string;
                /** @description The language to run the transcription in */
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
    "postAudio-apiV1TranscriptionAudioAudionameAudioidLanguage": {
        parameters: {
            query?: {
                /** @description The maximum number of speakers in the video */
                maxSpeaker?: number;
                /** @description The format of the audio file */
                format?: string;
            };
            header?: never;
            path: {
                /** @description The audio name to transcribe */
                audioName: string;
                /** @description The audio id to transcribe */
                audioId: number;
                /** @description The language to run the transcription in */
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
    "getAudio-apiV1TranscriptionAudioAudioidLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The audio id to transcribe */
                audioId: number;
                /** @description The language to run the transcription in */
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
                    "application/json": components["schemas"]["TranscriptionResultDTO"];
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
            405: {
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
