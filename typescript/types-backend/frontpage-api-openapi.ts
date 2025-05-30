export type paths = {
    "/frontpage-api/v1/subjectpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Fetch all subjectpages */
        get: operations["getFrontpage-apiV1Subjectpage"];
        put?: never;
        /** Create new subject page */
        post: operations["postFrontpage-apiV1Subjectpage"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/frontpage-api/v1/subjectpage/ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Fetch subject pages that matches ids parameter */
        get: operations["getFrontpage-apiV1SubjectpageIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/frontpage-api/v1/subjectpage/{subjectpage-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get data to display on a subject page */
        get: operations["getFrontpage-apiV1SubjectpageSubjectpage-id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /** Update subject page */
        patch: operations["patchFrontpage-apiV1SubjectpageSubjectpage-id"];
        trace?: never;
    };
    "/frontpage-api/v1/subjectpage/{subjectpage-id}/language/{language}": {
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
         * Delete language from subject page
         * @description Delete language from subject page
         */
        delete: operations["deleteFrontpage-apiV1SubjectpageSubjectpage-idLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/frontpage-api/v1/frontpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get data to display on the front page */
        get: operations["getFrontpage-apiV1Frontpage"];
        put?: never;
        /** Create front page */
        post: operations["postFrontpage-apiV1Frontpage"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/frontpage-api/v1/filmfrontpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get data to display on the film front page */
        get: operations["getFrontpage-apiV1Filmfrontpage"];
        put?: never;
        /** Update film front page */
        post: operations["postFrontpage-apiV1Filmfrontpage"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/frontpage-api/v1/filmfrontpage/language/{language}": {
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
         * Delete language from film front page
         * @description Delete language from film front page
         */
        delete: operations["deleteFrontpage-apiV1FilmfrontpageLanguageLanguage"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
};
export type webhooks = Record<string, never>;
export type components = {
    schemas: {
        /** AboutFilmSubjectDTO */
        AboutFilmSubjectDTO: {
            title: string;
            description: string;
            visualElement: components["schemas"]["VisualElementDTO"];
            language: string;
        };
        /** AboutSubjectDTO */
        AboutSubjectDTO: {
            title: string;
            description: string;
            visualElement: components["schemas"]["VisualElementDTO"];
        };
        /** AllErrors */
        AllErrors: components["schemas"]["ErrorBody"] | components["schemas"]["NotFoundWithSupportedLanguages"] | components["schemas"]["ValidationErrorBody"];
        /** BannerImageDTO */
        BannerImageDTO: {
            mobileUrl?: string;
            /** Format: int64 */
            mobileId?: number;
            desktopUrl: string;
            /** Format: int64 */
            desktopId: number;
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
        /** FilmFrontPageDTO */
        FilmFrontPageDTO: {
            name: string;
            about: components["schemas"]["AboutFilmSubjectDTO"][];
            movieThemes: components["schemas"]["MovieThemeDTO"][];
            slideShow: string[];
            article?: string;
            supportedLanguages: string[];
        };
        /**
         * FrontPageDTO
         * @description Object containing frontpage data
         */
        FrontPageDTO: {
            /**
             * Format: int64
             * @description Id of the frontpage
             */
            articleId: number;
            /** @description List of Menu objects */
            menu: components["schemas"]["MenuDTO"][];
        };
        /**
         * MenuDTO
         * @description The Menu object
         */
        MenuDTO: {
            /**
             * Format: int64
             * @description Id of the article
             */
            articleId: number;
            /** @description List of submenu objects */
            menu: components["schemas"]["MenuDataDTO"][];
            /** @description Hide this level in menu */
            hideLevel?: boolean;
        };
        /** MenuDataDTO */
        MenuDataDTO: components["schemas"]["MenuDTO"];
        /** MovieThemeDTO */
        MovieThemeDTO: {
            name: components["schemas"]["MovieThemeNameDTO"][];
            movies: string[];
        };
        /** MovieThemeNameDTO */
        MovieThemeNameDTO: {
            name: string;
            language: string;
        };
        /** NewOrUpdateBannerImageDTO */
        NewOrUpdateBannerImageDTO: {
            /** Format: int64 */
            mobileImageId?: number;
            /** Format: int64 */
            desktopImageId: number;
        };
        /** NewOrUpdatedAboutSubjectDTO */
        NewOrUpdatedAboutSubjectDTO: {
            title: string;
            description: string;
            language: string;
            visualElement: components["schemas"]["NewOrUpdatedVisualElementDTO"];
        };
        /** NewOrUpdatedFilmFrontPageDTO */
        NewOrUpdatedFilmFrontPageDTO: {
            name: string;
            about: components["schemas"]["NewOrUpdatedAboutSubjectDTO"][];
            movieThemes: components["schemas"]["NewOrUpdatedMovieThemeDTO"][];
            slideShow: string[];
            article?: string;
        };
        /** NewOrUpdatedMetaDescriptionDTO */
        NewOrUpdatedMetaDescriptionDTO: {
            metaDescription: string;
            language: string;
        };
        /** NewOrUpdatedMovieNameDTO */
        NewOrUpdatedMovieNameDTO: {
            name: string;
            language: string;
        };
        /** NewOrUpdatedMovieThemeDTO */
        NewOrUpdatedMovieThemeDTO: {
            name: components["schemas"]["NewOrUpdatedMovieNameDTO"][];
            movies: string[];
        };
        /** NewOrUpdatedVisualElementDTO */
        NewOrUpdatedVisualElementDTO: {
            type: string;
            id: string;
            alt?: string;
        };
        /** NewSubjectPageDTO */
        NewSubjectPageDTO: {
            name: string;
            externalId?: string;
            banner: components["schemas"]["NewOrUpdateBannerImageDTO"];
            about: components["schemas"]["NewOrUpdatedAboutSubjectDTO"][];
            metaDescription: components["schemas"]["NewOrUpdatedMetaDescriptionDTO"][];
            editorsChoices?: string[];
            connectedTo?: string[];
            buildsOn?: string[];
            leadsTo?: string[];
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
        /** SubjectPageDTO */
        SubjectPageDTO: {
            /** Format: int64 */
            id: number;
            name: string;
            banner: components["schemas"]["BannerImageDTO"];
            about?: components["schemas"]["AboutSubjectDTO"];
            metaDescription?: string;
            editorsChoices: string[];
            supportedLanguages: string[];
            connectedTo: string[];
            buildsOn: string[];
            leadsTo: string[];
        };
        /** UpdatedSubjectPageDTO */
        UpdatedSubjectPageDTO: {
            name?: string;
            externalId?: string;
            banner?: components["schemas"]["NewOrUpdateBannerImageDTO"];
            about?: components["schemas"]["NewOrUpdatedAboutSubjectDTO"][];
            metaDescription?: components["schemas"]["NewOrUpdatedMetaDescriptionDTO"][];
            editorsChoices?: string[];
            connectedTo?: string[];
            buildsOn?: string[];
            leadsTo?: string[];
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
        /** VisualElementDTO */
        VisualElementDTO: {
            type: string;
            url: string;
            alt?: string;
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
    "getFrontpage-apiV1Subjectpage": {
        parameters: {
            query?: {
                /** @description The page number of the search hits to display. */
                page?: number;
                /** @description The number of search hits to display for each page. */
                "page-size"?: number;
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
                    "application/json": components["schemas"]["SubjectPageDTO"][];
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
    "postFrontpage-apiV1Subjectpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewSubjectPageDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SubjectPageDTO"];
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
    "getFrontpage-apiV1SubjectpageIds": {
        parameters: {
            query?: {
                /** @description Return only subject pages that have one of the provided ids. To provide multiple ids, separate by comma (,). */
                ids?: number[];
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
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
                    "application/json": components["schemas"]["SubjectPageDTO"][];
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
    "getFrontpage-apiV1SubjectpageSubjectpage-id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description The subjectpage id */
                "subjectpage-id": number;
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
                    "application/json": components["schemas"]["SubjectPageDTO"];
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
    "patchFrontpage-apiV1SubjectpageSubjectpage-id": {
        parameters: {
            query?: {
                /** @description The ISO 639-1 language code describing language. */
                language?: string;
                /** @description Fallback to existing language if language is specified. */
                fallback?: boolean;
            };
            header?: never;
            path: {
                /** @description The subjectpage id */
                "subjectpage-id": number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedSubjectPageDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SubjectPageDTO"];
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
    "deleteFrontpage-apiV1SubjectpageSubjectpage-idLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The subjectpage id */
                "subjectpage-id": number;
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
                    "application/json": components["schemas"]["SubjectPageDTO"];
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
    "getFrontpage-apiV1Frontpage": {
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
                    "application/json": components["schemas"]["FrontPageDTO"];
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
    "postFrontpage-apiV1Frontpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["FrontPageDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FrontPageDTO"];
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
    "getFrontpage-apiV1Filmfrontpage": {
        parameters: {
            query?: {
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
                    "application/json": components["schemas"]["FilmFrontPageDTO"];
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
    "postFrontpage-apiV1Filmfrontpage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewOrUpdatedFilmFrontPageDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FilmFrontPageDTO"];
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
        };
    };
    "deleteFrontpage-apiV1FilmfrontpageLanguageLanguage": {
        parameters: {
            query?: never;
            header?: never;
            path: {
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
                    "application/json": components["schemas"]["FilmFrontPageDTO"];
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
