export type paths = {
    "/oembed-proxy/v1/oembed": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Returns oEmbed information for a given url.
         * @description Returns oEmbed information for a given url.
         */
        get: operations["getOembed-proxyV1Oembed"];
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
         * OEmbedDTO
         * @description oEmbed information for an url.
         */
        OEmbedDTO: {
            /** @description The resource type */
            type: string;
            /** @description The oEmbed version number. This must be 1.0. */
            version: string;
            /** @description A text title, describing the resource. */
            title?: string;
            /** @description A text description, describing the resource. Not standard. */
            description?: string;
            /** @description The name of the author/owner of the resource. */
            authorName?: string;
            /** @description A URL for the author/owner of the resource. */
            authorUrl?: string;
            /** @description The name of the resource provider. */
            providerName?: string;
            /** @description The url of the resource provider. */
            providerUrl?: string;
            /**
             * Format: int64
             * @description The suggested cache lifetime for this resource, in seconds. Consumers may choose to use this value or not.
             */
            cacheAge?: number;
            /** @description A URL to a thumbnail image representing the resource. The thumbnail must respect any maxwidth and maxheight parameters. If this parameter is present, thumbnail_width and thumbnail_height must also be present. */
            thumbnailUrl?: string;
            /**
             * Format: int64
             * @description The width of the optional thumbnail. If this parameter is present, thumbnail_url and thumbnail_height must also be present.
             */
            thumbnailWidth?: number;
            /**
             * Format: int64
             * @description The height of the optional thumbnail. If this parameter is present, thumbnail_url and thumbnail_width must also be present.
             */
            thumbnailHeight?: number;
            /** @description The source URL of the image. Consumers should be able to insert this URL into an <img> element. Only HTTP and HTTPS URLs are valid. Required if type is photo. */
            url?: string;
            /**
             * Format: int64
             * @description The width in pixels. Required if type is photo/video/rich
             */
            width?: number;
            /**
             * Format: int64
             * @description The height in pixels. Required if type is photo/video/rich
             */
            height?: number;
            /** @description The HTML required to embed a video player. The HTML should have no padding or margins. Consumers may wish to load the HTML in an off-domain iframe to avoid XSS vulnerabilities. Required if type is video/rich. */
            html?: string;
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
    "getOembed-proxyV1Oembed": {
        parameters: {
            query: {
                /** @description The URL to retrieve embedding information for */
                url: string;
                /** @description The maximum width of the embedded resource */
                maxwidth?: string;
                /** @description The maximum height of the embedded resource */
                maxheight?: string;
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
                    "application/json": components["schemas"]["OEmbedDTO"];
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
            410: {
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
}
