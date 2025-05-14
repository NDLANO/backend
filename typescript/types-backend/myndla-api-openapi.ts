export type paths = {
    "/myndla-api/v1/folders": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch top folders that belongs to a user
         * @description Fetch top folders that belongs to a user
         */
        get: operations["getMyndla-apiV1Folders"];
        put?: never;
        /**
         * Creates new folder
         * @description Creates new folder
         */
        post: operations["postMyndla-apiV1Folders"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch all resources that belongs to a user
         * @description Fetch all resources that belongs to a user
         */
        get: operations["getMyndla-apiV1FoldersResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/resources/recent": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch the most recent favorited resource
         * @description Fetch the most recent favorited resource
         */
        get: operations["getMyndla-apiV1FoldersResourcesRecent"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/{folder-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch a folder and all its content
         * @description Fetch a folder and all its content
         */
        get: operations["getMyndla-apiV1FoldersFolder-id"];
        put?: never;
        post?: never;
        /**
         * Remove folder from user folders
         * @description Remove folder from user folders
         */
        delete: operations["deleteMyndla-apiV1FoldersFolder-id"];
        options?: never;
        head?: never;
        /**
         * Update folder with new data
         * @description Update folder with new data
         */
        patch: operations["patchMyndla-apiV1FoldersFolder-id"];
        trace?: never;
    };
    "/myndla-api/v1/folders/{folder-id}/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Creates new folder resource
         * @description Creates new folder resource
         */
        post: operations["postMyndla-apiV1FoldersFolder-idResources"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/resources/{resource-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Updated selected resource
         * @description Updates selected resource
         */
        patch: operations["patchMyndla-apiV1FoldersResourcesResource-id"];
        trace?: never;
    };
    "/myndla-api/v1/folders/{folder-id}/resources/{resource-id}": {
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
         * Delete selected resource
         * @description Delete selected resource
         */
        delete: operations["deleteMyndla-apiV1FoldersFolder-idResourcesResource-id"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/shared/{folder-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Fetch a shared folder and all its content
         * @description Fetch a shared folder and all its content
         */
        get: operations["getMyndla-apiV1FoldersSharedFolder-id"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Change status for given folder and all its subfolders
         * @description Change status for given folder and all its subfolders
         */
        patch: operations["patchMyndla-apiV1FoldersSharedFolder-id"];
        trace?: never;
    };
    "/myndla-api/v1/folders/clone/{source-folder-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Creates new folder structure based on source folder structure
         * @description Creates new folder structure based on source folder structure
         */
        post: operations["postMyndla-apiV1FoldersCloneSource-folder-id"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/sort-resources/{folder-id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Decide order of resource ids in a folder
         * @description Decide order of resource ids in a folder
         */
        put: operations["putMyndla-apiV1FoldersSort-resourcesFolder-id"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/sort-subfolders": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Decide order of subfolder ids in a folder
         * @description Decide order of subfolder ids in a folder
         */
        put: operations["putMyndla-apiV1FoldersSort-subfolders"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/sort-saved": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Decide order of saved shared folders
         * @description Decide order of saved shared folders
         */
        put: operations["putMyndla-apiV1FoldersSort-saved"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/folders/shared/{folder-id}/save": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Saves a shared folder
         * @description Saves a shared folder
         */
        post: operations["postMyndla-apiV1FoldersSharedFolder-idSave"];
        /**
         * Deletes a saved shared folder
         * @description Deletes a saved shared folder
         */
        delete: operations["deleteMyndla-apiV1FoldersSharedFolder-idSave"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get user data
         * @description Get user data
         */
        get: operations["getMyndla-apiV1Users"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Update user data
         * @description Update user data
         */
        patch: operations["patchMyndla-apiV1Users"];
        trace?: never;
    };
    "/myndla-api/v1/users/delete-personal-data": {
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
         * Delete all data connected to this user
         * @description Delete all data connected to this user
         */
        delete: operations["deleteMyndla-apiV1UsersDelete-personal-data"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/users/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Export all stored user-related data as a json structure
         * @description Export all stored user-related data as a json structure
         */
        get: operations["getMyndla-apiV1UsersExport"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/users/import": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Import all stored user-related data from a exported json structure
         * @description Import all stored user-related data from a exported json structure
         */
        post: operations["postMyndla-apiV1UsersImport"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/config/{config-key}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get db configuration by key
         * @description Get db configuration by key
         */
        get: operations["getMyndla-apiV1ConfigConfig-key"];
        put?: never;
        /**
         * Update configuration used by api.
         * @description Update configuration used by api.
         */
        post: operations["postMyndla-apiV1ConfigConfig-key"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/stats": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get stats
         * @description Get stats
         */
        get: operations["getMyndla-apiV1Stats"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/stats/favorites": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get number of favorites for favorited resources
         * @description Get number of favorites for favorited resources
         */
        get: operations["getMyndla-apiV1StatsFavorites"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/myndla-api/v1/stats/favorites/{resourceType}/{resourceIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get folder resource favorites
         * @description Get folder resource favorites
         */
        get: operations["getMyndla-apiV1StatsFavoritesResourcetypeResourceids"];
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
        /** BreadcrumbDTO */
        BreadcrumbDTO: {
            /** @description UUID of the folder */
            id: string;
            /** @description Folder name */
            name: string;
        };
        /**
         * ConfigKey
         * @enum {string}
         */
        ConfigKey: "LEARNINGPATH_WRITE_RESTRICTED" | "MY_NDLA_WRITE_RESTRICTED";
        /**
         * ConfigMetaDTO
         * @description Describes configuration value.
         */
        ConfigMetaDTO: {
            /** @description Configuration key */
            key: string;
            /** @description Configuration value. */
            value: string[] | boolean;
            /** @description Date of when configuration was last updated */
            updatedAt: string;
            /** @description UserId of who last updated the configuration parameter. */
            updatedBy: string;
        };
        /**
         * ConfigMetaRestrictedDTO
         * @description Describes configuration value.
         */
        ConfigMetaRestrictedDTO: {
            /** @description Configuration key */
            key: string;
            /** @description Configuration value. */
            value: string[] | boolean;
        };
        /** ConfigMetaValueDTO */
        ConfigMetaValueDTO: {
            /** @description Value to set configuration param to. */
            value: string[] | boolean;
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
        /** ExportedUserDataDTO */
        ExportedUserDataDTO: {
            /** @description The users data */
            userData: components["schemas"]["MyNDLAUserDTO"];
            /** @description The users folders */
            folders: components["schemas"]["FolderDTO"][];
        };
        /** FolderDTO */
        FolderDTO: {
            /** @description UUID of the folder */
            id: string;
            /** @description Folder name */
            name: string;
            /** @description Folder status */
            status: string;
            /** @description UUID of parent folder */
            parentId?: string;
            /** @description List of parent folders to resource */
            breadcrumbs: components["schemas"]["BreadcrumbDTO"][];
            /** @description List of subfolders */
            subfolders: components["schemas"]["FolderDataDTO"][];
            /** @description List of resources */
            resources: components["schemas"]["ResourceDTO"][];
            /**
             * Format: int32
             * @description Where the folder is sorted within its parent
             */
            rank: number;
            /** @description When the folder was created */
            created: string;
            /** @description When the folder was updated */
            updated: string;
            /** @description When the folder was last shared */
            shared?: string;
            /** @description Description of the folder */
            description?: string;
            owner?: components["schemas"]["OwnerDTO"];
        };
        /** FolderDataDTO */
        FolderDataDTO: components["schemas"]["FolderDTO"];
        /** FolderSortRequestDTO */
        FolderSortRequestDTO: {
            /** @description List of the children ids in sorted order, MUST be all ids */
            sortedIds: string[];
        };
        /**
         * FolderStatus
         * @enum {string}
         */
        FolderStatus: "private" | "shared";
        /** Map_Long */
        Map_Long: {
            [key: string]: number;
        };
        /** Map_Map_String_Long */
        Map_Map_String_Long: {
            [key: string]: components["schemas"]["Map_Long"];
        };
        /** MyNDLAGroupDTO */
        MyNDLAGroupDTO: {
            /** @description ID of the group */
            id: string;
            /** @description Name of the group */
            displayName: string;
            /** @description Is this the primary school */
            isPrimarySchool: boolean;
            /** @description ID of parent group */
            parentId?: string;
        };
        /** MyNDLAUserDTO */
        MyNDLAUserDTO: {
            /**
             * Format: int64
             * @description ID of the user
             */
            id: number;
            /** @description FeideID of the user */
            feideId: string;
            /** @description Username of the user */
            username: string;
            /** @description Email address of the user */
            email: string;
            /** @description Name of the user */
            displayName: string;
            /** @description Favorite subjects of the user */
            favoriteSubjects: string[];
            role: components["schemas"]["UserRole"];
            /** @description User root organization */
            organization: string;
            /** @description User groups */
            groups: components["schemas"]["MyNDLAGroupDTO"][];
            /** @description Whether arena is explicitly enabled for the user */
            arenaEnabled: boolean;
            /** @description Whether the user has accepted the use of arena */
            arenaAccepted: boolean;
            /** @description Whether the user has accepted the sharing of their name */
            shareNameAccepted: boolean;
        };
        /** NewFolderDTO */
        NewFolderDTO: {
            /** @description Folder name */
            name: string;
            /** @description Id of parent folder */
            parentId?: string;
            /** @description Status of the folder (private, shared) */
            status?: string;
            /** @description Description of the folder */
            description?: string;
        };
        /** NewResourceDTO */
        NewResourceDTO: {
            /** @description Type of the resource. (Article, Learningpath) */
            resourceType: components["schemas"]["ResourceType"];
            /** @description Relative path of this resource */
            path: string;
            /** @description List of tags */
            tags?: string[];
            /** @description The id of the resource, useful for fetching metadata for the resource */
            resourceId: string;
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
         * OwnerDTO
         * @description Owner of the folder, if the owner have opted in to share their name
         */
        OwnerDTO: {
            /** @description Name of the owner */
            name: string;
        };
        /** ResourceDTO */
        ResourceDTO: {
            /** @description Unique ID of the resource */
            id: string;
            /** @description Type of the resource. (Article, Learningpath) */
            resourceType: components["schemas"]["ResourceType"];
            /** @description Relative path of this resource */
            path: string;
            /** @description When the resource was created */
            created: string;
            /** @description List of tags */
            tags: string[];
            /** @description The id of the resource, useful for fetching metadata for the resource */
            resourceId: string;
            /**
             * Format: int32
             * @description The which rank the resource appears in a sorted sequence
             */
            rank?: number;
        };
        /** ResourceStatsDTO */
        ResourceStatsDTO: {
            /** @description The type of favourited resouce */
            type: string;
            /**
             * Format: int64
             * @description The number of favourited resource
             */
            number: number;
        };
        /**
         * ResourceType
         * @enum {string}
         */
        ResourceType: "article" | "audio" | "concept" | "image" | "learningpath" | "multidisciplinary" | "topic" | "video";
        /**
         * SingleResourceStatsDTO
         * @description Stats for single resource
         */
        SingleResourceStatsDTO: {
            /** @description Id of the resource */
            id: string;
            /**
             * Format: int64
             * @description The number of times the resource has been favorited
             */
            favourites: number;
        };
        /**
         * StatsDTO
         * @description Stats for my-ndla usage
         */
        StatsDTO: {
            /**
             * Format: int64
             * @description The total number of users registered
             */
            numberOfUsers: number;
            /**
             * Format: int64
             * @description The total number of created folders
             */
            numberOfFolders: number;
            /**
             * Format: int64
             * @description The total number of favourited resources
             */
            numberOfResources: number;
            /**
             * Format: int64
             * @description The total number of created tags
             */
            numberOfTags: number;
            /**
             * Format: int64
             * @description The total number of favourited subjects
             */
            numberOfSubjects: number;
            /**
             * Format: int64
             * @description The total number of shared folders
             */
            numberOfSharedFolders: number;
            /**
             * Format: int64
             * @description The total number of learning paths in My NDLA
             */
            numberOfMyNdlaLearningPaths: number;
            /** @description Stats for type resources */
            favouritedResources: components["schemas"]["ResourceStatsDTO"][];
            /** @description Stats for favourited resources */
            favourited: components["schemas"]["Map_Long"];
            users: components["schemas"]["UserStatsDTO"];
        };
        /** UpdatedFolderDTO */
        UpdatedFolderDTO: {
            /** @description Folder name */
            name?: string;
            /** @description Status of the folder (private, shared) */
            status?: string;
            /** @description Description of the folder */
            description?: string;
        };
        /** UpdatedMyNDLAUserDTO */
        UpdatedMyNDLAUserDTO: {
            /** @description Favorite subjects of the user */
            favoriteSubjects?: string[];
            /** @description Whether arena should explicitly be enabled for the user */
            arenaEnabled?: boolean;
            /** @description Whether the user has accepted the use of arena */
            arenaAccepted?: boolean;
            /** @description Whether the user has accepted the sharing of their name */
            shareNameAccepted?: boolean;
        };
        /** UpdatedResourceDTO */
        UpdatedResourceDTO: {
            /** @description List of tags */
            tags?: string[];
            /** @description The id of the resource, useful for fetching metadata for the resource */
            resourceId?: string;
        };
        /**
         * UserFolderDTO
         * @description User folder data
         */
        UserFolderDTO: {
            /** @description The users own folders */
            folders: components["schemas"]["FolderDTO"][];
            /** @description The shared folder the user has saved */
            sharedFolders: components["schemas"]["FolderDTO"][];
        };
        /**
         * UserRole
         * @description User role
         * @enum {string}
         */
        UserRole: "employee" | "student";
        /**
         * UserStatsDTO
         * @description Stats for the users
         */
        UserStatsDTO: {
            /**
             * Format: int64
             * @description The total number of users
             */
            total: number;
            /**
             * Format: int64
             * @description The number of employees
             */
            employees: number;
            /**
             * Format: int64
             * @description The number of students
             */
            students: number;
            /**
             * Format: int64
             * @description The number of users with favourites
             */
            withFavourites: number;
            /**
             * Format: int64
             * @description The number of users with no favourites
             */
            noFavourites: number;
            /**
             * Format: int64
             * @description The number of users in the arena
             */
            arena: number;
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
    "getMyndla-apiV1Folders": {
        parameters: {
            query?: {
                /** @description Choose if resources should be included in the response */
                "include-resources"?: boolean;
                /** @description Choose if sub-folders should be included in the response */
                "include-subfolders"?: boolean;
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
                    "application/json": components["schemas"]["UserFolderDTO"];
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
    "postMyndla-apiV1Folders": {
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
                "application/json": components["schemas"]["NewFolderDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FolderDTO"];
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
    "getMyndla-apiV1FoldersResources": {
        parameters: {
            query?: {
                /** @description Limit the number of results to this many elements */
                size?: number;
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
                    "application/json": components["schemas"]["ResourceDTO"][];
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
    "getMyndla-apiV1FoldersResourcesRecent": {
        parameters: {
            query?: {
                /** @description How many latest favorited resources to return */
                size?: number;
                /** @description Which resource types to exclude. If None all resource types are included. To provide multiple resource types, separate by comma (,). */
                exclude?: components["schemas"]["ResourceType"][];
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
                    "application/json": components["schemas"]["ResourceDTO"][];
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
    "getMyndla-apiV1FoldersFolder-id": {
        parameters: {
            query?: {
                /** @description Choose if resources should be included in the response */
                "include-resources"?: boolean;
                /** @description Choose if sub-folders should be included in the response */
                "include-subfolders"?: boolean;
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
                    "application/json": components["schemas"]["FolderDTO"];
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
    "deleteMyndla-apiV1FoldersFolder-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
    "patchMyndla-apiV1FoldersFolder-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedFolderDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FolderDTO"];
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
    "postMyndla-apiV1FoldersFolder-idResources": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NewResourceDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ResourceDTO"];
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
    "patchMyndla-apiV1FoldersResourcesResource-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the resource */
                "resource-id": string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdatedResourceDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ResourceDTO"];
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
    "deleteMyndla-apiV1FoldersFolder-idResourcesResource-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
                /** @description The UUID of the resource */
                "resource-id": string;
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
    "getMyndla-apiV1FoldersSharedFolder-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
                    "application/json": components["schemas"]["FolderDTO"];
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
    "patchMyndla-apiV1FoldersSharedFolder-id": {
        parameters: {
            query: {
                /** @description Status of the folder */
                "folder-status": components["schemas"]["FolderStatus"];
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
    "postMyndla-apiV1FoldersCloneSource-folder-id": {
        parameters: {
            query?: {
                /** @description Destination UUID of the folder. If None it will be cloned as a root folder. */
                "destination-folder-id"?: string;
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description Source UUID of the folder. */
                "source-folder-id": string;
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
                    "application/json": components["schemas"]["FolderDTO"];
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
    "putMyndla-apiV1FoldersSort-resourcesFolder-id": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["FolderSortRequestDTO"];
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
    "putMyndla-apiV1FoldersSort-subfolders": {
        parameters: {
            query?: {
                /** @description The UUID of the folder */
                "folder-id"?: string;
            };
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["FolderSortRequestDTO"];
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
    "putMyndla-apiV1FoldersSort-saved": {
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
                "application/json": components["schemas"]["FolderSortRequestDTO"];
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
    "postMyndla-apiV1FoldersSharedFolder-idSave": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
    "deleteMyndla-apiV1FoldersSharedFolder-idSave": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
            path: {
                /** @description The UUID of the folder */
                "folder-id": string;
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
    "getMyndla-apiV1Users": {
        parameters: {
            query?: never;
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
                    "application/json": components["schemas"]["MyNDLAUserDTO"];
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
    "patchMyndla-apiV1Users": {
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
                "application/json": components["schemas"]["UpdatedMyNDLAUserDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MyNDLAUserDTO"];
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
    "deleteMyndla-apiV1UsersDelete-personal-data": {
        parameters: {
            query?: never;
            header?: {
                /** @description Header containing FEIDE access token. */
                FeideAuthorization?: string;
            };
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
    "getMyndla-apiV1UsersExport": {
        parameters: {
            query?: never;
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
                    "application/json": components["schemas"]["ExportedUserDataDTO"];
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
    "postMyndla-apiV1UsersImport": {
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
                "application/json": components["schemas"]["ExportedUserDataDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExportedUserDataDTO"];
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
    "getMyndla-apiV1ConfigConfig-key": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The of configuration value. Can only be one of 'LEARNINGPATH_WRITE_RESTRICTED', 'MY_NDLA_WRITE_RESTRICTED' */
                "config-key": components["schemas"]["ConfigKey"];
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
                    "application/json": components["schemas"]["ConfigMetaRestrictedDTO"];
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
    "postMyndla-apiV1ConfigConfig-key": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The of configuration value. Can only be one of 'LEARNINGPATH_WRITE_RESTRICTED', 'MY_NDLA_WRITE_RESTRICTED' */
                "config-key": components["schemas"]["ConfigKey"];
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConfigMetaValueDTO"];
            };
        };
        responses: {
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ConfigMetaDTO"];
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
    "getMyndla-apiV1Stats": {
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
                    "application/json": components["schemas"]["StatsDTO"];
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
    "getMyndla-apiV1StatsFavorites": {
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
                    "application/json": components["schemas"]["Map_Map_String_Long"];
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
    "getMyndla-apiV1StatsFavoritesResourcetypeResourceids": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The type of the resource to look up. Comma separated list to support multidisciplinary. Possible values article, audio, concept, image, learningpath, multidisciplinary, topic, video */
                resourceType: string[];
                /** @description IDs of the resources to look up */
                resourceIds: string[];
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
                    "application/json": components["schemas"]["SingleResourceStatsDTO"][];
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
