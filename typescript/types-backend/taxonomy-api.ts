export type paths = {
    "/v1/admin/buildAverageTree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Updates average tree for all nodes. Requires taxonomy:admin access. */
        post: operations["buildAverageTree"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/admin/buildAverageTree/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Updates average tree for the provided node. Requires taxonomy:admin access. */
        post: operations["buildAverageTree_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/admin/buildContexts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Updates contexts for all roots. Requires taxonomy:admin access. */
        get: operations["buildAllContexts"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/contexts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets id of all nodes registered as context */
        get: operations["getAllContexts"];
        put?: never;
        /**
         * Registers a new node as context
         * @description All subjects are already contexts and may not be added again. The node to register as context must exist already.
         */
        post: operations["createContext"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/contexts/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets id of all nodes registered as context */
        get: operations["getAllContexts_1"];
        put?: never;
        /**
         * Registers a new node as context
         * @description All subjects are already contexts and may not be added again. The node to register as context must exist already.
         */
        post: operations["createContext_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/contexts/{id}": {
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
         * Removes context registration from node
         * @description Does not remove the underlying node, only marks it as not being a context
         */
        delete: operations["deleteContext"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-connections": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and children */
        get: operations["getAllNodeConnections"];
        put?: never;
        /** Adds a node to a parent */
        post: operations["createNodeConnection"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-connections/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and children */
        get: operations["getAllNodeConnections_1"];
        put?: never;
        /** Adds a node to a parent */
        post: operations["createNodeConnection_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-connections/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and children paginated */
        get: operations["getNodeConnectionsPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-connections/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single connection between a node and a child */
        get: operations["getNodeConnection"];
        /**
         * Updates a connection between a node and a child
         * @description Use to update which node is primary to a child or to alter sorting order
         */
        put: operations["updateNodeConnection"];
        post?: never;
        /** Removes a connection between a node and a child */
        delete: operations["deleteEntity_7"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-connections/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata_5"];
        /** Updates metadata for entity */
        put: operations["putMetadata_5"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and resources */
        get: operations["getAllNodeResources"];
        put?: never;
        /** Adds a resource to a node */
        post: operations["createNodeResource"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and resources */
        get: operations["getAllNodeResources_1"];
        put?: never;
        /** Adds a resource to a node */
        post: operations["createNodeResource_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-resources/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between node and resources paginated */
        get: operations["getNodeResourcesPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-resources/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a specific connection between a node and a resource */
        get: operations["getNodeResource"];
        /**
         * Updates a connection between a node and a resource
         * @description Use to update which node is primary to the resource or to change sorting order.
         */
        put: operations["updateNodeResource"];
        post?: never;
        /** Removes a resource from a node */
        delete: operations["deleteEntity_6"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/node-resources/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata_4"];
        /** Updates metadata for entity */
        put: operations["putMetadata_4"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all nodes */
        get: operations["getAllNodes"];
        put?: never;
        /** Creates a new node */
        post: operations["createNode"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all nodes */
        get: operations["getAllNodes_1"];
        put?: never;
        /** Creates a new node */
        post: operations["createNode_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all nodes paginated */
        get: operations["getNodePage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Search all nodes */
        get: operations["searchNodes"];
        put?: never;
        /** Search all nodes */
        post: operations["searchNodes_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single node */
        get: operations["getNode"];
        /** Updates a single node */
        put: operations["updateNode"];
        post?: never;
        /** Deletes a single node by id */
        delete: operations["deleteEntity_5"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/clone": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Clones a node, presumably a resource, including resource-types and translations */
        post: operations["cloneResource_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/connections": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all parents and children this node is connected to */
        get: operations["getAllConnections"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/full": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets node including information about all parents, and resourceTypes for this resource. Can be replaced with regular get-endpoint and traversing contexts
         * @deprecated
         */
        get: operations["getNodeFull"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/makeResourcesPrimary": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** Makes all connected resources primary */
        put: operations["makeResourcesPrimary_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata_3"];
        /** Updates metadata for entity */
        put: operations["putMetadata_3"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/nodes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all children for this node */
        get: operations["getChildren"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/publish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Publishes a node hierarchy to a version
         * @deprecated
         */
        put: operations["publishNode"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all resources for the given node */
        get: operations["getResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all translations for a single node */
        get: operations["getAllNodeTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all translations for a single node */
        get: operations["getAllNodeTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/nodes/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single translation for a single node */
        get: operations["getNodeTranslation"];
        /** Creates or updates a translation of a node */
        put: operations["createUpdateNodeTranslation"];
        post?: never;
        /** Deletes a translation */
        delete: operations["deleteNodeTranslation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/queries/contextId": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a list of contexts matching given contextId, empty list if no matches are found. */
        get: operations["contextByContextId"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/queries/path": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a list of contexts matching given pretty url with contextId, empty list if no matches are found. */
        get: operations["queryPath"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/queries/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead
         * @deprecated
         */
        get: operations["queryResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/queries/topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead
         * @deprecated
         */
        get: operations["queryTopics"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/queries/{contentURI}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a list of contexts matching given contentURI, empty list if no matches are found. */
        get: operations["contextByContentURI"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevances */
        get: operations["getAllRelevances"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevances */
        get: operations["getAllRelevances_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single relevance
         * @description Default language will be returned if desired language not found or if parameter is omitted.
         */
        get: operations["getRelevance"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevanceTranslations for a single relevance */
        get: operations["getAllRelevanceTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevanceTranslations for a single relevance */
        get: operations["getAllRelevanceTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/relevances/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single translation for a single relevance */
        get: operations["getRelevanceTranslation"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-resourcetypes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between resources and resource types */
        get: operations["getAllResourceResourceTypes"];
        put?: never;
        /** Adds a resource type to a resource */
        post: operations["createResourceResourceType"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-resourcetypes/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all connections between resources and resource types */
        get: operations["getAllResourceResourceTypes_1"];
        put?: never;
        /** Adds a resource type to a resource */
        post: operations["createResourceResourceType_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-resourcetypes/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single connection between resource and resource type */
        get: operations["getResourceResourceType"];
        put?: never;
        post?: never;
        /** Removes a resource type from a resource */
        delete: operations["deleteResourceResourceType"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a list of all resource types */
        get: operations["getAllResourceTypes"];
        put?: never;
        /** Adds a new resource type */
        post: operations["createResourceType"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a list of all resource types */
        get: operations["getAllResourceTypes_1"];
        put?: never;
        /** Adds a new resource type */
        post: operations["createResourceType_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single resource type */
        get: operations["getResourceType"];
        /** Updates a resource type. Use to update which resource type is parent. You can also update the id, take care! */
        put: operations["updateResourceType"];
        post?: never;
        /** Deletes a single entity by id */
        delete: operations["deleteEntity_4"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/{id}/subtypes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets subtypes of one resource type */
        get: operations["getResourceTypeSubtypes"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevanceTranslations for a single resource type */
        get: operations["getAllResourceTypeTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all relevanceTranslations for a single resource type */
        get: operations["getAllResourceTypeTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resource-types/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single translation for a single resource type */
        get: operations["getResourceTypeTranslation"];
        /** Creates or updates a translation of a resource type */
        put: operations["createUpdateResourceTypeTranslation"];
        post?: never;
        /** Deletes a translation */
        delete: operations["deleteResourceTypeTranslation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Lists all resources
         * @deprecated
         */
        get: operations["getAllResources"];
        put?: never;
        /**
         * Adds a new resource
         * @deprecated
         */
        post: operations["createResource"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Lists all resources
         * @deprecated
         */
        get: operations["getAllResources_1"];
        put?: never;
        /**
         * Adds a new resource
         * @deprecated
         */
        post: operations["createResource_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all resources paginated
         * @deprecated
         */
        get: operations["getResourcePage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Search all resources
         * @deprecated
         */
        get: operations["searchResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single resource
         * @deprecated
         */
        get: operations["getResource"];
        /**
         * Updates a resource
         * @deprecated
         */
        put: operations["updateResource"];
        post?: never;
        /**
         * Deletes a single entity by id
         * @deprecated
         */
        delete: operations["deleteEntity_3"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/clone": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Clones a resource, including resource-types and translations
         * @deprecated
         */
        post: operations["cloneResource"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/full": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all parent topics, all filters and resourceTypes for this resource
         * @deprecated
         */
        get: operations["getResourceFull"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata_2"];
        /** Updates metadata for entity */
        put: operations["putMetadata_2"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/resource-types": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all resource types associated with this resource
         * @deprecated
         */
        get: operations["getResourceTypes"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single resource
         * @deprecated
         */
        get: operations["getAllResourceTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single resource
         * @deprecated
         */
        get: operations["getAllResourceTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/resources/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single translation for a single resource
         * @deprecated
         */
        get: operations["getResourceTranslation"];
        /**
         * Creates or updates a translation of a resource
         * @deprecated
         */
        put: operations["createUpdateResourceTranslation"];
        post?: never;
        /**
         * Deletes a translation
         * @deprecated
         */
        delete: operations["deleteResourceTranslation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subject-topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between subjects and topics
         * @deprecated
         */
        get: operations["getAllSubjectTopics"];
        put?: never;
        /**
         * Adds a new topic to a subject
         * @deprecated
         */
        post: operations["createSubjectTopic"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subject-topics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between subjects and topics
         * @deprecated
         */
        get: operations["getAllSubjectTopics_1"];
        put?: never;
        /**
         * Adds a new topic to a subject
         * @deprecated
         */
        post: operations["createSubjectTopic_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subject-topics/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between subjects and topics paginated
         * @deprecated
         */
        get: operations["getSubjectTopicPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subject-topics/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a specific connection between a subject and a topic
         * @deprecated
         */
        get: operations["getSubjectTopic"];
        /**
         * Updates a connection between subject and topic
         * @deprecated
         * @description Use to update which subject is primary to a topic or to change sorting order.
         */
        put: operations["updateSubjectTopic"];
        post?: never;
        /**
         * Removes a topic from a subject
         * @deprecated
         */
        delete: operations["deleteSubjectTopic"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all subjects
         * @deprecated
         */
        get: operations["getAllSubjects"];
        put?: never;
        /**
         * Creates a new subject
         * @deprecated
         */
        post: operations["createSubject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all subjects
         * @deprecated
         */
        get: operations["getAllSubjects_1"];
        put?: never;
        /**
         * Creates a new subject
         * @deprecated
         */
        post: operations["createSubject_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all nodes paginated
         * @deprecated
         */
        get: operations["getSubjectPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Search all subjects
         * @deprecated
         */
        get: operations["searchSubjects"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single subject
         * @deprecated
         * @description Default language will be returned if desired language not found or if parameter is omitted.
         */
        get: operations["getSubject"];
        /**
         * Updates a subject
         * @deprecated
         */
        put: operations["updateSubject"];
        post?: never;
        /**
         * Deletes a single entity by id
         * @deprecated
         */
        delete: operations["deleteEntity_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata_1"];
        /** Updates metadata for entity */
        put: operations["putMetadata_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}/topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all children associated with a subject
         * @deprecated
         * @description This resource is read-only. To update the relationship between nodes, use the resource /subject-topics.
         */
        get: operations["getSubjectChildren"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single subject
         * @deprecated
         */
        get: operations["getAllSubjectTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single subject
         * @deprecated
         */
        get: operations["getAllSubjectTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single translation for a single subject
         * @deprecated
         */
        get: operations["getSubjectTranslation"];
        /**
         * Creates or updates a translation of a subject
         * @deprecated
         */
        put: operations["createUpdateSubjectTranslation"];
        post?: never;
        /**
         * Deletes a translation
         * @deprecated
         */
        delete: operations["deleteSubjectTranslation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/subjects/{subjectId}/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all resources for a subject. Searches recursively in all children of this node.The ordering of resources will be based on the rank of resources relative to the node they belong to.
         * @deprecated
         */
        get: operations["getSubjectResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topics and resources
         * @deprecated
         */
        get: operations["getAllTopicResources"];
        put?: never;
        /**
         * Adds a resource to a topic
         * @deprecated
         */
        post: operations["createTopicResource"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topics and resources
         * @deprecated
         */
        get: operations["getAllTopicResources_1"];
        put?: never;
        /**
         * Adds a resource to a topic
         * @deprecated
         */
        post: operations["createTopicResource_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-resources/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topic and resources paginated
         * @deprecated
         */
        get: operations["getTopicResourcePage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-resources/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a specific connection between a topic and a resource
         * @deprecated
         */
        get: operations["getTopicResource"];
        /**
         * Updates a connection between a topic and a resource
         * @deprecated
         * @description Use to update which topic is primary to the resource or to change sorting order.
         */
        put: operations["updateTopicResource"];
        post?: never;
        /**
         * Removes a resource from a topic
         * @deprecated
         */
        delete: operations["deleteTopicResource"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-subtopics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topics and subtopics
         * @deprecated
         */
        get: operations["getAllTopicSubtopics"];
        put?: never;
        /**
         * Adds a subtopic to a topic
         * @deprecated
         */
        post: operations["createTopicSubtopic"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-subtopics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topics and subtopics
         * @deprecated
         */
        get: operations["getAllTopicSubtopics_1"];
        put?: never;
        /**
         * Adds a subtopic to a topic
         * @deprecated
         */
        post: operations["createTopicSubtopic_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-subtopics/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all connections between topics and subtopics paginated
         * @deprecated
         */
        get: operations["getTopicSubtopicPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topic-subtopics/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single connection between a topic and a subtopic
         * @deprecated
         */
        get: operations["getTopicSubtopic"];
        /**
         * Updates a connection between a topic and a subtopic
         * @deprecated
         * @description Use to update which topic is primary to a subtopic or to alter sorting order
         */
        put: operations["updateTopicSubtopic"];
        post?: never;
        /**
         * Removes a connection between a topic and a subtopic
         * @deprecated
         */
        delete: operations["deleteTopicSubtopic"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all topics
         * @deprecated
         */
        get: operations["getAllTopics"];
        put?: never;
        /**
         * Creates a new topic
         * @deprecated
         */
        post: operations["createTopic"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all topics
         * @deprecated
         */
        get: operations["getAllTopics_1"];
        put?: never;
        /**
         * Creates a new topic
         * @deprecated
         */
        post: operations["createTopic_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all topics paginated
         * @deprecated
         */
        get: operations["getTopicsPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Search all topics
         * @deprecated
         */
        get: operations["searchTopics"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single topic
         * @deprecated
         */
        get: operations["getTopic"];
        /**
         * Updates a single topic
         * @deprecated
         */
        put: operations["updateTopic"];
        post?: never;
        /**
         * @deprecated
         * @description Deletes a single entity by id
         */
        delete: operations["deleteEntity_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/connections": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all subjects and subtopics this topic is connected to
         * @deprecated
         */
        get: operations["getAllTopicConnections"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/makeResourcesPrimary": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Makes all connected resources primary
         * @deprecated
         */
        put: operations["makeResourcesPrimary"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/metadata": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets metadata for entity */
        get: operations["getMetadata"];
        /** Updates metadata for entity */
        put: operations["putMetadata"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all resources for the given topic
         * @deprecated
         */
        get: operations["getTopicResources"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all subtopics for this topic
         * @deprecated
         */
        get: operations["getTopicSubTopics"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single topic
         * @deprecated
         */
        get: operations["getAllTopicTranslations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets all relevanceTranslations for a single topic
         * @deprecated
         */
        get: operations["getAllTopicTranslations_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/topics/{id}/translations/{language}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Gets a single translation for a single topic
         * @deprecated
         */
        get: operations["getTopicTranslation"];
        /**
         * Creates or updates a translation of a topic
         * @deprecated
         */
        put: operations["createUpdateTopicTranslation"];
        post?: never;
        /**
         * Deletes a translation
         * @deprecated
         */
        delete: operations["deleteTopicTranslation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/url/mapping": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Returns path for an url or HTTP 404 */
        get: operations["getTaxonomyPathForUrl"];
        /** Inserts or updates a mapping from url to nodeId and optionally subjectId */
        put: operations["putTaxonomyNodeAndSubjectForOldUrl"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/url/resolve": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["resolve"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/versions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all versions */
        get: operations["getAllVersions"];
        put?: never;
        /** Creates a new version */
        post: operations["createVersion"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/versions/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets all versions */
        get: operations["getAllVersions_1"];
        put?: never;
        /** Creates a new version */
        post: operations["createVersion_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/versions/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Gets a single version */
        get: operations["getVersion"];
        /** Updates a version */
        put: operations["updateVersion"];
        post?: never;
        /** Deletes a version by publicId */
        delete: operations["deleteEntity"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/versions/{id}/publish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** Publishes a version */
        put: operations["publishVersion"];
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
        Connection: {
            /**
             * Format: uri
             * @description The id of the subject-topic or topic-subtopic connection
             * @example urn:subject-topic:1
             */
            connectionId: string;
            /**
             * @deprecated
             * @description True if owned by this topic, false if it has its primary connection elsewhere
             * @example true
             */
            isPrimary: boolean;
            /**
             * @deprecated
             * @description The path part of the url for the subject or subtopic connected to this topic
             * @example /subject:1/topic:1
             */
            paths: string[];
            /**
             * Format: uri
             * @description The id of the connected subject or topic
             * @example urn:subject:1
             */
            targetId: string;
            /** @description The type of connection (parent, child, referrer or target */
            type: string;
        };
        Context: {
            /** Format: uri */
            id: string;
            name: string;
            path: string;
        };
        /** @description object containing public id of the node to be registered as context */
        ContextPOST: {
            /** Format: uri */
            id: string;
        };
        /**
         * Format: int32
         * @enum {integer}
         */
        Grade: 1 | 2 | 3 | 4 | 5;
        GradeAverage: {
            /** Format: double */
            averageValue: number;
            /** Format: int32 */
            count: number;
        };
        LanguageFieldString: {
            [key: string]: string;
        };
        Metadata: {
            customFields: {
                [key: string]: string;
            };
            grepCodes: string[];
            visible: boolean;
        };
        MetadataPUT: {
            /** @description Custom fields, Only updated if present */
            customFields?: {
                [key: string]: string;
            };
            /** @description Set of grep codes, Only updated if present */
            grepCodes?: string[];
            /** @description Visibility of the node, Only updated if present */
            visible?: boolean;
        };
        Node: {
            /**
             * @description The stored name of the node
             * @example Trigonometry
             */
            baseName: string;
            /** @description List of names in the path */
            breadcrumbs: string[];
            /**
             * Format: uri
             * @description ID of content introducing this node. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri?: string;
            /** @description The context object selected when fetching node */
            context?: components["schemas"]["TaxonomyContext"];
            /** @description An id unique for this context. */
            contextId?: string;
            /** @description A list of all contextids this node has ever had */
            contextids: string[];
            /** @description A list of all contexts this node is part of */
            contexts: components["schemas"]["TaxonomyContext"][];
            /** @description A pretty url based on the name and context in the default language. */
            defaultUrl?: string;
            /** @description Url safe name for the node in the default language. */
            defaultUrlName?: string;
            /** @description A number representing the average grade of all children nodes recursively. */
            gradeAverage?: components["schemas"]["GradeAverage"];
            /**
             * Format: uri
             * @description Node id
             * @example urn:topic:234
             */
            id: string;
            /**
             * @description The language code for which name is returned
             * @example nb
             */
            language: string;
            /** @description Metadata for entity. Read only. */
            metadata: components["schemas"]["Metadata"];
            /**
             * @description The possibly translated name of the node
             * @example Trigonometry
             */
            name: string;
            /**
             * @description The type of node
             * @example resource
             */
            nodeType: components["schemas"]["NodeType"];
            /**
             * @description The primary path for this node. Can be empty if no context
             * @example /subject:1/topic:1
             */
            path?: string;
            /** @description List of all paths to this node */
            paths: string[];
            /** @description Quality evaluation of the article */
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * @description Resource type(s)
             * @example [
             *       {
             *         "id": "urn:resourcetype:1",
             *         "name": "lecture"
             *       }
             *     ]
             */
            resourceTypes: components["schemas"]["ResourceTypeWithConnection"][];
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description The technical evaluation of the node. */
            technicalEvaluation?: components["schemas"]["TechnicalEvaluationDTO"];
            /** @description All translations of this node */
            translations: components["schemas"]["Translation"][];
            /** @description A pretty url based on name and context. Empty if no context. */
            url?: string;
            /** @description Url safe names for the node. */
            urlName?: string[];
        };
        NodeChild: {
            /**
             * @description The stored name of the node
             * @example Trigonometry
             */
            baseName: string;
            /** @description List of names in the path */
            breadcrumbs: string[];
            /**
             * Format: uri
             * @description The id of the node connection which causes this node to be included in the result set.
             * @example urn:node-connection:1
             */
            connectionId: string;
            /**
             * Format: uri
             * @description ID of content introducing this node. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri?: string;
            /** @description The context object selected when fetching node */
            context?: components["schemas"]["TaxonomyContext"];
            /** @description An id unique for this context. */
            contextId?: string;
            /** @description A list of all contextids this node has ever had */
            contextids: string[];
            /** @description A list of all contexts this node is part of */
            contexts: components["schemas"]["TaxonomyContext"][];
            /** @description A pretty url based on the name and context in the default language. */
            defaultUrl?: string;
            /** @description Url safe name for the node in the default language. */
            defaultUrlName?: string;
            /** @description A number representing the average grade of all children nodes recursively. */
            gradeAverage?: components["schemas"]["GradeAverage"];
            /**
             * Format: uri
             * @description Node id
             * @example urn:topic:234
             */
            id: string;
            isPrimary: boolean;
            /**
             * @description The language code for which name is returned
             * @example nb
             */
            language: string;
            /** @description Metadata for entity. Read only. */
            metadata: components["schemas"]["Metadata"];
            /**
             * @description The possibly translated name of the node
             * @example Trigonometry
             */
            name: string;
            /**
             * @description The type of node
             * @example resource
             */
            nodeType: components["schemas"]["NodeType"];
            /**
             * Format: uri
             * @description Parent id in the current context, null if none exists
             */
            parentId: string;
            /**
             * @description The primary path for this node. Can be empty if no context
             * @example /subject:1/topic:1
             */
            path?: string;
            /** @description List of all paths to this node */
            paths: string[];
            /** @description Quality evaluation of the article */
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /**
             * Format: int32
             * @description The order in which to sort the node within it's level.
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * @description Resource type(s)
             * @example [
             *       {
             *         "id": "urn:resourcetype:1",
             *         "name": "lecture"
             *       }
             *     ]
             */
            resourceTypes: components["schemas"]["ResourceTypeWithConnection"][];
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description The technical evaluation of the node. */
            technicalEvaluation?: components["schemas"]["TechnicalEvaluationDTO"];
            /** @description All translations of this node */
            translations: components["schemas"]["Translation"][];
            /** @description A pretty url based on name and context. Empty if no context. */
            url?: string;
            /** @description Url safe names for the node. */
            urlName?: string[];
        };
        NodeConnection: {
            /**
             * Format: uri
             * @description Child id
             * @example urn:topic:234
             */
            childId: string;
            /** @description Connection type */
            connectionType: components["schemas"]["NodeConnectionType"];
            /**
             * Format: uri
             * @description Connection id
             * @example urn:topic-has-subtopics:345
             */
            id: string;
            /** @description Metadata for entity. Read only. */
            metadata: components["schemas"]["Metadata"];
            /**
             * Format: uri
             * @description Parent id
             * @example urn:topic:234
             */
            parentId: string;
            /**
             * @description Is this connection primary
             * @example true
             */
            primary: boolean;
            /**
             * Format: int32
             * @description Order in which subtopic is sorted for the topic
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        /** @description The new connection */
        NodeConnectionPOST: {
            /**
             * Format: uri
             * @description Child id
             * @example urn:topic:234
             */
            childId: string;
            /**
             * @description Connection type
             * @default BRANCH
             * @example BRANCH
             */
            connectionType?: components["schemas"]["NodeConnectionType"];
            /**
             * Parent id
             * Format: uri
             * @example urn:topic:234
             */
            parentId: string;
            /**
             * @description If this connection is primary.
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which to sort the child for the parent
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        /** @description The updated connection */
        NodeConnectionPUT: {
            /**
             * @description If this connection is primary.
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which subtopic is sorted for the topic
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        /** @enum {string} */
        NodeConnectionType: "BRANCH" | "LINK";
        /** @description The new node */
        NodePostPut: {
            /**
             * Format: uri
             * @description ID of content introducing this node. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri?: string;
            /** @description The node is the root in a context. Default is false. Only used if present. */
            context?: boolean;
            /**
             * @description The language used at create time. Used to set default translation.
             * @example nb
             */
            language?: string;
            /**
             * @description The name of the node. Required on create.
             * @example Trigonometry
             */
            name?: string;
            /** @description If specified, set the node_id to this value. If omitted, an uuid will be assigned automatically. */
            nodeId?: string;
            /**
             * @description Type of node.
             * @example topic
             */
            nodeType?: components["schemas"]["NodeType"];
            /** @description The quality evaluation of the node. Consist of a score from 1 to 5 and a comment. Can be null to remove existing evaluation. */
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /**
             * @deprecated
             * @description The node is a root node. Default is false. Only used if present.
             */
            root?: boolean;
            /** @description The technical evaluation of the node. Contains a flag and an optional comment. Can be null to remove existing evaluation. */
            technicalEvaluation?: components["schemas"]["TechnicalEvaluationDTO"];
            /** @description The node is visible. Default is true. */
            visible?: boolean;
        };
        NodeResource: {
            /**
             * Format: uri
             * @description Node resource connection id
             * @example urn:node-resource:123
             */
            id: string;
            /** @description Metadata for entity. Read only. */
            metadata: components["schemas"]["Metadata"];
            /**
             * Format: uri
             * @description Node id
             * @example urn:node:345
             */
            nodeId: string;
            /**
             * @description Primary connection
             * @example true
             */
            primary: boolean;
            /**
             * Format: int32
             * @description Order in which the resource is sorted for the node
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Resource id
             * @example urn:resource:345
             */
            resourceId: string;
        };
        /** @description new node/resource connection */
        NodeResourcePOST: {
            /**
             * Format: uri
             * @description Node id
             * @example urn:node:345
             */
            nodeId: string;
            /**
             * @description Primary connection
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which resource is sorted for the node
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Resource id
             * @example urn:resource:345
             */
            resourceId: string;
        };
        /** @description Updated node/resource connection */
        NodeResourcePUT: {
            /**
             * @description Primary connection
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which the resource will be sorted for this node.
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        NodeSearchBody: {
            /** @description ContentURIs to fetch for query */
            contentUris?: string[];
            /** @description If specified, the search result will be filtered by whether they include the key,value combination provided. If more than one provided only one will be required (OR) */
            customFields?: {
                [key: string]: string;
            };
            /** @description Filter out programme contexts */
            filterProgrammes?: boolean;
            /** @description Ids to fetch for query */
            ids?: string[];
            /** @description Include all contexts */
            includeContexts?: boolean;
            /**
             * @description ISO-639-1 language code
             * @example nb
             */
            language?: string;
            /** @description Filter by nodeType */
            nodeType?: components["schemas"]["NodeType"][];
            /**
             * Format: int32
             * @description Which page to fetch
             */
            page?: number;
            /**
             * Format: int32
             * @description How many results to return per page
             */
            pageSize?: number;
            /**
             * Format: uri
             * @description Id to parent id in context.
             */
            parentId?: string;
            /** @description Query to search names */
            query?: string;
            /**
             * Format: uri
             * @description Id to root id in context.
             */
            rootId?: string;
        };
        /** @enum {string} */
        NodeType: "NODE" | "SUBJECT" | "TOPIC" | "CASE" | "RESOURCE" | "PROGRAMME";
        NodeWithParents: {
            /**
             * @description The stored name of the node
             * @example Trigonometry
             */
            baseName?: string;
            /** @description List of names in the path */
            breadcrumbs?: string[];
            /**
             * Format: uri
             * @description ID of content introducing this node. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri?: string;
            /** @description The context object selected when fetching node */
            context?: components["schemas"]["TaxonomyContext"];
            /** @description An id unique for this context. */
            contextId?: string;
            /** @description A list of all contextids this node has ever had */
            contextids?: string[];
            /** @description A list of all contexts this node is part of */
            contexts?: components["schemas"]["TaxonomyContext"][];
            /** @description A pretty url based on the name and context in the default language. */
            defaultUrl?: string;
            /** @description Url safe name for the node in the default language. */
            defaultUrlName?: string;
            /** @description A number representing the average grade of all children nodes recursively. */
            gradeAverage?: components["schemas"]["GradeAverage"];
            /**
             * Format: uri
             * @description Node id
             * @example urn:topic:234
             */
            id?: string;
            /**
             * @description The language code for which name is returned
             * @example nb
             */
            language?: string;
            /** @description Metadata for entity. Read only. */
            metadata?: components["schemas"]["Metadata"];
            /**
             * @description The possibly translated name of the node
             * @example Trigonometry
             */
            name?: string;
            /**
             * @description The type of node
             * @example resource
             */
            nodeType?: components["schemas"]["NodeType"];
            /**
             * @description Parent topology nodes and whether or not connection type is primary
             * @example [
             *       {
             *         "id": "urn:topic:1:181900",
             *         "name": "I dyrehagen",
             *         "contentUri": "urn:article:6662",
             *         "path": "/subject:2/topic:1:181900",
             *         "primary": "true"
             *       }
             *     ]
             */
            parents: components["schemas"]["NodeChild"][];
            /**
             * @description The primary path for this node. Can be empty if no context
             * @example /subject:1/topic:1
             */
            path?: string;
            /** @description List of all paths to this node */
            paths?: string[];
            /** @description Quality evaluation of the article */
            qualityEvaluation?: components["schemas"]["QualityEvaluationDTO"];
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * @description Resource type(s)
             * @example [
             *       {
             *         "id": "urn:resourcetype:1",
             *         "name": "lecture"
             *       }
             *     ]
             */
            resourceTypes?: components["schemas"]["ResourceTypeWithConnection"][];
            /** @description List of language codes supported by translations */
            supportedLanguages?: string[];
            /** @description The technical evaluation of the node. */
            technicalEvaluation?: components["schemas"]["TechnicalEvaluationDTO"];
            /** @description All translations of this node */
            translations?: components["schemas"]["Translation"][];
            /** @description A pretty url based on name and context. Empty if no context. */
            url?: string;
            /** @description Url safe names for the node. */
            urlName?: string[];
        };
        QualityEvaluationDTO: {
            /** @description The grade (1-5) of the article */
            grade: components["schemas"]["Grade"];
            /** @description Note explaining the score */
            note?: string;
        };
        Relevance: {
            /**
             * Format: uri
             * @description Specifies if node is core or supplementary
             * @example urn:relevance:core
             */
            id: string;
            /**
             * @description The name of the relevance
             * @example Core
             */
            name: string;
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description All translations of this relevance */
            translations: components["schemas"]["Translation"][];
        };
        ResolvedOldUrl: {
            /**
             * @description URL path for resource
             * @example '/subject:1/topic:12/resource:12'
             */
            path: string;
        };
        ResolvedUrl: {
            /**
             * Format: uri
             * @description The ID of this element in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier for the system, and <id> is the id of this content in that system.
             * @example urn:article:1
             */
            contentUri: string;
            /** @description Is this an exact match for the provided path? False if this is another path to the same resource. */
            exactMatch: boolean;
            /**
             * Format: uri
             * @description ID of the element referred to by the given path
             * @example urn:resource:1
             */
            id: string;
            /**
             * @description Element name. For performance reasons, this name is for informational purposes only. To get a translated name, please fetch the resolved resource using its rest resource.
             * @example Basic physics
             */
            name: string;
            /** @description Parent elements of the resolved element. The first element is the parent, the second is the grandparent, etc. */
            parents: string[];
            /**
             * @description URL path for resource
             * @example '/subject:1/topic:12/resource:12'
             */
            path: string;
            /**
             * @description Pretty url resource
             * @example '/r/subject-name/resource-name/hash'
             */
            url: string;
        };
        /** @description Object containing contentUri. Other values are ignored. */
        ResourcePOST: {
            /**
             * Format: uri
             * @description The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier for the system, and <id> is the id of this content in that system.
             * @example urn:article:1
             */
            contentUri: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:resource:2
             */
            id?: string;
            /**
             * @description The name of the resource
             * @example Introduction to integration
             */
            name: string;
        };
        /** @description the new resource */
        ResourcePUT: {
            /**
             * Format: uri
             * @description The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier for the system, and <id> is the id of this content in that system.
             * @example urn:article:1
             */
            contentUri: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:resource:2
             */
            id?: string;
            /**
             * @description The name of the resource
             * @example Introduction to integration
             */
            name: string;
        };
        ResourceResourceType: {
            /**
             * Format: uri
             * @description Resource to resource type connection id
             * @example urn:resource-has-resourcetypes:12
             */
            id: string;
            /**
             * Format: uri
             * @description Resource type id
             * @example urn:resource:123
             */
            resourceId: string;
            /**
             * Format: uri
             * @description Resource type id
             * @example urn:resourcetype:234
             */
            resourceTypeId: string;
        };
        /** @description The new resource/resource type connection */
        ResourceResourceTypePOST: {
            /**
             * Format: uri
             * @description Resource id
             * @example urn:resource:123
             */
            resourceId: string;
            /**
             * Format: uri
             * @description Resource type id
             * @example urn:resourcetype:234
             */
            resourceTypeId: string;
        };
        ResourceType: {
            /**
             * Format: uri
             * @example urn:resourcetype:1
             */
            id: string;
            /**
             * @description The name of the resource type
             * @example Lecture
             */
            name: string;
            /**
             * Format: int32
             * @description Sort order of the resource type
             * @example 1
             */
            order?: number;
            /** @description Sub resource types */
            subtypes?: components["schemas"]["ResourceType"][];
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description All translations of this resource type */
            translations: components["schemas"]["Translation"][];
        };
        /** @description The new resource type */
        ResourceTypePUT: {
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:resourcetype:1
             */
            id: string;
            /**
             * @description The name of the resource type
             * @example Lecture
             */
            name: string;
            /**
             * Format: int32
             * @description Order in which the resource type should be sorted among its siblings
             */
            order?: number;
            /**
             * Format: uri
             * @description If specified, the new resource type will be a child of the mentioned resource type.
             */
            parentId: string;
        };
        ResourceTypeWithConnection: {
            /**
             * Format: uri
             * @description The id of the resource resource type connection
             * @example urn:resource-resourcetype:1
             */
            connectionId: string;
            /**
             * Format: uri
             * @example urn:resourcetype:2
             */
            id: string;
            /**
             * @description The name of the resource type
             * @example Lecture
             */
            name: string;
            /**
             * Format: int32
             * @description Internal order of the resource types
             */
            order?: number;
            /**
             * Format: uri
             * @example urn:resourcetype:1
             */
            parentId?: string;
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description All translations of this resource type */
            translations: components["schemas"]["Translation"][];
        };
        SearchResult: {
            /**
             * Format: int32
             * @example The page number
             */
            page: number;
            /**
             * Format: int32
             * @example The page size
             */
            pageSize: number;
            /** @example List of search results */
            results: components["schemas"]["Node"][];
            /**
             * Format: int64
             * @example Total search result count, useful for fetching multiple pages
             */
            totalCount: number;
        };
        SearchableTaxonomyResourceType: {
            id: string;
            name: {
                [key: string]: string;
            };
            /** Format: int32 */
            order?: number;
            parentId?: string;
        };
        /** @description The updated subject. Fields not included will be set to null. */
        SubjectPOST: {
            /**
             * Format: uri
             * @description ID of frontpage connected to this subject. Must be a valid URI, but preferably not a URL.
             * @example urn:frontpage:1
             */
            contentUri?: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:subject:1
             */
            id?: string;
            /**
             * @description The name of the subject
             * @example Mathematics
             */
            name: string;
        };
        /** @description The new subject */
        SubjectPUT: {
            /**
             * Format: uri
             * @description ID of frontpage connected to this subject. Must be a valid URI, but preferably not a URL.
             * @example urn:frontpage:1
             */
            contentUri?: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:subject:1
             */
            id?: string;
            /**
             * @description The name of the subject
             * @example Mathematics
             */
            name: string;
        };
        SubjectTopic: {
            /**
             * Format: uri
             * @description Connection id
             * @example urn:subject-has-topics:34
             */
            id: string;
            /**
             * @description primary
             * @example true
             */
            primary: boolean;
            /**
             * Format: int32
             * @description Order in which the topic is sorted under the subject
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Subject id
             * @example urn:subject:123
             */
            subjectid: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:345
             */
            topicid: string;
        };
        /** @description The subject and topic getting connected. */
        SubjectTopicPOST: {
            /**
             * @description Backwards compatibility: Always true, ignored on insert/update.
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which the topic should be sorted for the topic
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Subject id
             * @example urn:subject:123
             */
            subjectid: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:234
             */
            topicid: string;
        };
        /** @description updated subject/topic connection */
        SubjectTopicPUT: {
            /**
             * @description If true, set this subject as the primary subject for this topic. This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which the topic should be sorted for the subject
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        TaxonomyContext: {
            /** @description A breadcrumb of the names of the context's parents */
            breadcrumbs: {
                [key: string]: string[];
            };
            /** @description The id of the parent connection object */
            connectionId: string;
            /** @description Unique id of context based on root + parent connection */
            contextId: string;
            /** @description Whether a 'standard'-article, 'topic-article'-article or a 'learningpath' */
            contextType?: string;
            /** @description Pretty-url of this particular context in the default language */
            defaultUrl: string;
            /**
             * Format: uri
             * @description The publicId of the node connected via content-uri
             */
            id: string;
            /** @description Whether the parent connection is marked as active */
            isActive: boolean;
            /** @description Whether the root is marked as archived */
            isArchived: boolean;
            /** @description Whether the parent connection is primary or not */
            isPrimary: boolean;
            /** @description Whether the parent connection is visible or not */
            isVisible: boolean;
            /** @description List of all parent contextIds */
            parentContextIds: string[];
            /** @description List of all parent ids */
            parentIds: string[];
            /** @description List of all parents to this context. Empty if node is fetched as child */
            parents: components["schemas"]["TaxonomyCrumb"][];
            /** @description The context path */
            path: string;
            /**
             * Format: uri
             * @deprecated
             * @description The publicId of the node connected via content-uri
             */
            publicId: string;
            /**
             * Format: int32
             * @description The rank of the parent connection object
             */
            rank: number;
            /** @description Name of the relevance of the parent connection */
            relevance: components["schemas"]["LanguageFieldString"];
            /**
             * Format: uri
             * @description Id of the relevance of the parent connection
             */
            relevanceId: string;
            /** @description Resource-types of the node */
            resourceTypes: components["schemas"]["SearchableTaxonomyResourceType"][];
            /** @description The name of the root parent of the context */
            root: {
                [key: string]: string;
            };
            /**
             * Format: uri
             * @description The publicId of the root parent of the context
             */
            rootId: string;
            /** @description Pretty-url of this particular context */
            url: string;
        };
        TaxonomyCrumb: {
            /** @description Unique id of context based on root + parent connection */
            contextId: string;
            /**
             * Format: uri
             * @description The publicId of the node
             */
            id: string;
            /** @description The name of the node */
            name: components["schemas"]["LanguageFieldString"];
            /** @description The context path */
            path: string;
            /** @description The context url */
            url: string;
        };
        TechnicalEvaluationDTO: {
            /** @description Notes for the technical evaluation of this node. */
            comment?: string;
            /** @description Whether this node requires a technical evaluation. */
            requiresEvaluation: boolean;
        };
        /** @description The new topic */
        TopicPOST: {
            /**
             * Format: uri
             * @description ID of article introducing this topic. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:topic:1
             */
            id?: string;
            /**
             * @description The name of the topic
             * @example Trigonometry
             */
            name: string;
        };
        /** @description The updated topic. Fields not included will be set to null. */
        TopicPostPut: {
            /**
             * Format: uri
             * @description ID of article introducing this topic. Must be a valid URI, but preferably not a URL.
             * @example urn:article:1
             */
            contentUri: string;
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:topic:1
             */
            id?: string;
            /**
             * @description The name of the topic
             * @example Trigonometry
             */
            name: string;
        };
        TopicResource: {
            /**
             * Format: uri
             * @description Topic resource connection id
             * @example urn:topic-has-resources:123
             */
            id: string;
            /**
             * @description Primary connection
             * @example true
             */
            primary: boolean;
            /**
             * Format: int32
             * @description Order in which the resource is sorted for the topic
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Resource id
             * @example urn:resource:345
             */
            resourceId: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:345
             */
            topicid: string;
        };
        /** @description new topic/resource connection */
        TopicResourcePOST: {
            /**
             * @description Primary connection
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which resource is sorted for the topic
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Resource id
             * @example urn:resource:345
             */
            resourceId: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:345
             */
            topicid: string;
        };
        /** @description Updated topic/resource connection */
        TopicResourcePUT: {
            /**
             * @description Primary connection
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which the resource will be sorted for this topic.
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        TopicSubtopic: {
            /**
             * Format: uri
             * @description Connection id
             * @example urn:topic-has-subtopics:345
             */
            id: string;
            /**
             * @description Backwards compatibility: Always true. Ignored on insert/update
             * @example true
             */
            primary: boolean;
            /**
             * Format: int32
             * @description Order in which subtopic is sorted for the topic
             * @example 1
             */
            rank: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Subtopic id
             * @example urn:topic:234
             */
            subtopicid: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:234
             */
            topicid: string;
        };
        /** @description The new connection */
        TopicSubtopicPOST: {
            /**
             * @description Is this connection primary
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which to sort the subtopic for the topic
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
            /**
             * Format: uri
             * @description Subtopic id
             * @example urn:topic:234
             */
            subtopicid: string;
            /**
             * Format: uri
             * @description Topic id
             * @example urn:topic:234
             */
            topicid: string;
        };
        /** @description The updated connection */
        TopicSubtopicPUT: {
            /**
             * @description Is this connection primary
             * @example true
             */
            primary?: boolean;
            /**
             * Format: int32
             * @description Order in which subtopic is sorted for the topic
             * @example 1
             */
            rank?: number;
            /**
             * Format: uri
             * @description Relevance id
             * @example urn:relevance:core
             */
            relevanceId?: string;
        };
        Translation: {
            /**
             * @description ISO 639-1 language code
             * @example en
             */
            language: string;
            /**
             * @description The translated name of the node
             * @example Trigonometry
             */
            name: string;
        };
        /** @description The new or updated translation */
        TranslationPUT: {
            /**
             * @description The translated name of the element. Used wherever translated texts are used.
             * @example Trigonometry
             */
            name: string;
        };
        UrlMapping: {
            /**
             * @description Node URN for resource in new system
             * @example urn:topic:1:183926
             */
            nodeId: string;
            /**
             * @description Subject URN for resource in new system (optional)
             * @example urn:subject:5
             */
            subjectId: string;
            /**
             * @description URL for resource in old system
             * @example ndla.no/nb/node/183926?fag=127013
             */
            url: string;
        };
        Version: {
            /**
             * Format: date-time
             * @description Timestamp for when version was archived
             */
            archived?: string;
            /**
             * Format: date-time
             * @description Timestamp for when version was created
             */
            created: string;
            /** @description Unique hash for the version */
            hash: string;
            /**
             * Format: uri
             * @example urn:version:1
             */
            id: string;
            /** @description Is the version locked */
            locked: boolean;
            /** @description Name for the version */
            name: string;
            /**
             * Format: date-time
             * @description Timestamp for when version was published
             */
            published?: string;
            /** @example BETA */
            versionType: components["schemas"]["VersionType"];
        };
        /** @description The new version */
        VersionPostPut: {
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:version: and be a valid URI. If ommitted, an id will be assigned automatically.
             * @example urn:version:1
             */
            id?: string;
            /** @description If specified, set the locked property to this value. */
            locked?: boolean;
            /**
             * @description If specified, set the name to this value.
             * @example Beta 2022
             */
            name: string;
        };
        /** @enum {string} */
        VersionType: "BETA" | "PUBLISHED" | "ARCHIVED";
    };
    responses: never;
    parameters: {
        versionHash: string;
    };
    requestBodies: never;
    headers: {
        /** @description versionHash */
        versionHash: string;
    };
    pathItems: never;
};
export type Connection = components['schemas']['Connection'];
export type Context = components['schemas']['Context'];
export type ContextPOST = components['schemas']['ContextPOST'];
export type Grade = components['schemas']['Grade'];
export type GradeAverage = components['schemas']['GradeAverage'];
export type LanguageFieldString = components['schemas']['LanguageFieldString'];
export type Metadata = components['schemas']['Metadata'];
export type MetadataPUT = components['schemas']['MetadataPUT'];
export type Node = components['schemas']['Node'];
export type NodeChild = components['schemas']['NodeChild'];
export type NodeConnection = components['schemas']['NodeConnection'];
export type NodeConnectionPOST = components['schemas']['NodeConnectionPOST'];
export type NodeConnectionPUT = components['schemas']['NodeConnectionPUT'];
export type NodeConnectionType = components['schemas']['NodeConnectionType'];
export type NodePostPut = components['schemas']['NodePostPut'];
export type NodeResource = components['schemas']['NodeResource'];
export type NodeResourcePOST = components['schemas']['NodeResourcePOST'];
export type NodeResourcePUT = components['schemas']['NodeResourcePUT'];
export type NodeSearchBody = components['schemas']['NodeSearchBody'];
export type NodeType = components['schemas']['NodeType'];
export type NodeWithParents = components['schemas']['NodeWithParents'];
export type QualityEvaluationDTO = components['schemas']['QualityEvaluationDTO'];
export type Relevance = components['schemas']['Relevance'];
export type ResolvedOldUrl = components['schemas']['ResolvedOldUrl'];
export type ResolvedUrl = components['schemas']['ResolvedUrl'];
export type ResourcePOST = components['schemas']['ResourcePOST'];
export type ResourcePUT = components['schemas']['ResourcePUT'];
export type ResourceResourceType = components['schemas']['ResourceResourceType'];
export type ResourceResourceTypePOST = components['schemas']['ResourceResourceTypePOST'];
export type ResourceType = components['schemas']['ResourceType'];
export type ResourceTypePUT = components['schemas']['ResourceTypePUT'];
export type ResourceTypeWithConnection = components['schemas']['ResourceTypeWithConnection'];
export type SearchResult = components['schemas']['SearchResult'];
export type SearchableTaxonomyResourceType = components['schemas']['SearchableTaxonomyResourceType'];
export type SubjectPOST = components['schemas']['SubjectPOST'];
export type SubjectPUT = components['schemas']['SubjectPUT'];
export type SubjectTopic = components['schemas']['SubjectTopic'];
export type SubjectTopicPOST = components['schemas']['SubjectTopicPOST'];
export type SubjectTopicPUT = components['schemas']['SubjectTopicPUT'];
export type TaxonomyContext = components['schemas']['TaxonomyContext'];
export type TaxonomyCrumb = components['schemas']['TaxonomyCrumb'];
export type TechnicalEvaluationDTO = components['schemas']['TechnicalEvaluationDTO'];
export type TopicPOST = components['schemas']['TopicPOST'];
export type TopicPostPut = components['schemas']['TopicPostPut'];
export type TopicResource = components['schemas']['TopicResource'];
export type TopicResourcePOST = components['schemas']['TopicResourcePOST'];
export type TopicResourcePUT = components['schemas']['TopicResourcePUT'];
export type TopicSubtopic = components['schemas']['TopicSubtopic'];
export type TopicSubtopicPOST = components['schemas']['TopicSubtopicPOST'];
export type TopicSubtopicPUT = components['schemas']['TopicSubtopicPUT'];
export type Translation = components['schemas']['Translation'];
export type TranslationPUT = components['schemas']['TranslationPUT'];
export type UrlMapping = components['schemas']['UrlMapping'];
export type Version = components['schemas']['Version'];
export type VersionPostPut = components['schemas']['VersionPostPut'];
export type VersionType = components['schemas']['VersionType'];
export type ParameterVersionHash = components['parameters']['versionHash'];
export type HeaderVersionHash = components['headers']['versionHash'];
export type $defs = Record<string, never>;
export interface operations {
    buildAverageTree: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    buildAverageTree_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    buildAllContexts: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Accepted */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllContexts: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Context"][];
                };
            };
        };
    };
    createContext: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ContextPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllContexts_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Context"][];
                };
            };
        };
    };
    createContext_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ContextPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteContext: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllNodeConnections: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeConnection"][];
                };
            };
        };
    };
    createNodeConnection: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeConnectionPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllNodeConnections_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeConnection"][];
                };
            };
        };
    };
    createNodeConnection_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeConnectionPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getNodeConnectionsPage: {
        parameters: {
            query?: {
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getNodeConnection: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeConnection"];
                };
            };
        };
    };
    updateNodeConnection: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeConnectionPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_7: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getMetadata_5: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata_5: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getAllNodeResources: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeResource"][];
                };
            };
        };
    };
    createNodeResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeResourcePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllNodeResources_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeResource"][];
                };
            };
        };
    };
    createNodeResource_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeResourcePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getNodeResourcesPage: {
        parameters: {
            query: {
                /** @description The page to fetch */
                page: number;
                /** @description Size of page to fetch */
                pageSize: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getNodeResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeResource"];
                };
            };
        };
    };
    updateNodeResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeResourcePUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_6: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getMetadata_4: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata_4: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getAllNodes: {
        parameters: {
            query?: {
                /** @description Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^) */
                nodeType?: components["schemas"]["NodeType"][];
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Ids to filter by */
                ids?: string[];
                /**
                 * @deprecated
                 * @description Only root level contexts
                 */
                isRoot?: boolean;
                /** @description Only contexts */
                isContext?: boolean;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter by context id. Beware: handled separately from other parameters! */
                contextId?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Id to root id in context. */
                rootId?: string;
                /** @description Id to parent id in context. */
                parentId?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createNode: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodePostPut"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllNodes_1: {
        parameters: {
            query?: {
                /** @description Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^) */
                nodeType?: components["schemas"]["NodeType"][];
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Ids to filter by */
                ids?: string[];
                /**
                 * @deprecated
                 * @description Only root level contexts
                 */
                isRoot?: boolean;
                /** @description Only contexts */
                isContext?: boolean;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter by context id. Beware: handled separately from other parameters! */
                contextId?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Id to root id in context. */
                rootId?: string;
                /** @description Id to parent id in context. */
                parentId?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createNode_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodePostPut"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getNodePage: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
                /** @description Filter by nodeType */
                nodeType?: components["schemas"]["NodeType"];
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    searchNodes: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description How many results to return per page */
                pageSize?: number;
                /** @description Which page to fetch */
                page?: number;
                /** @description Query to search names */
                query?: string;
                /** @description Ids to fetch for query */
                ids?: string[];
                /** @description ContentURIs to fetch for query */
                contentUris?: string[];
                /** @description Filter by nodeType */
                nodeType?: components["schemas"]["NodeType"][];
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Id to root id in context to select. Does not affect search results */
                rootId?: string;
                /** @description Id to parent id in context to select. Does not affect search results */
                parentId?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    searchNodes_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodeSearchBody"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getNode: {
        parameters: {
            query?: {
                /** @description Id to root id in context. */
                rootId?: string;
                /** @description Id to parent id in context. */
                parentId?: string;
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"];
                };
            };
        };
    };
    updateNode: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodePostPut"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_5: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    cloneResource_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                /**
                 * @description Id of node to clone
                 * @example urn:resource:1
                 */
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NodePostPut"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllConnections: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Connection"][];
                };
            };
        };
    };
    getNodeFull: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Include all contexts */
                includeContexts?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeWithParents"];
                };
            };
        };
    };
    makeResourcesPrimary_1: {
        parameters: {
            query?: {
                /** @description If true, children are fetched recursively */
                recursive?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": boolean;
                };
            };
        };
    };
    getMetadata_3: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata_3: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getChildren: {
        parameters: {
            query?: {
                /** @description Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^) */
                nodeType?: components["schemas"]["NodeType"][];
                /** @description Only connections of given type are returned */
                connectionTypes?: components["schemas"]["NodeConnectionType"][];
                /** @description If true, children are fetched recursively */
                recursive?: boolean;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    publishNode: {
        parameters: {
            query: {
                /**
                 * @description Version id to publish from. Can be omitted to publish from default.
                 * @example urn:version:1
                 */
                sourceId?: string;
                /**
                 * @description Version id to publish to.
                 * @example urn:version:2
                 */
                targetId: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Accepted */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResources: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Include all contexts */
                includeContexts?: boolean;
                /** @description Filter out programme contexts */
                filterProgrammes?: boolean;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
                /** @description If true, resources from children are fetched recursively */
                recursive?: boolean;
                /** @description Select by resource type id(s). If not specified, resources of all types will be returned. Multiple ids may be separated with comma or the parameter may be repeated for each id. */
                type?: string[];
                /** @description Select by relevance. If not specified, all resources will be returned. */
                relevance?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    getAllNodeTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllNodeTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getNodeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    createUpdateNodeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TranslationPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteNodeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    contextByContextId: {
        parameters: {
            query?: {
                contextId?: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TaxonomyContext"][];
                };
            };
        };
    };
    queryPath: {
        parameters: {
            query?: {
                path?: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TaxonomyContext"][];
                };
            };
        };
    };
    queryResources: {
        parameters: {
            query?: {
                contentURI?: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Fitler by key and value */
                value?: string;
                /** @description Filter by visible */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    queryTopics: {
        parameters: {
            query: {
                contentURI: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Fitler by key and value */
                value?: string;
                /** @description Filter by visible */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    contextByContentURI: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Whether to filter out contexts if a parent (or the node itself) is non-visible */
                filterVisibles?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                contentURI: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TaxonomyContext"][];
                };
            };
        };
    };
    getAllRelevances: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Relevance"][];
                };
            };
        };
    };
    getAllRelevances_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Relevance"][];
                };
            };
        };
    };
    getRelevance: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Relevance"];
                };
            };
        };
    };
    getAllRelevanceTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllRelevanceTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getRelevanceTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    getAllResourceResourceTypes: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceResourceType"][];
                };
            };
        };
    };
    createResourceResourceType: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourceResourceTypePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllResourceResourceTypes_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceResourceType"][];
                };
            };
        };
    };
    createResourceResourceType_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourceResourceTypePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResourceResourceType: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceResourceType"];
                };
            };
        };
    };
    deleteResourceResourceType: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllResourceTypes: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceType"][];
                };
            };
        };
    };
    createResourceType: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourceTypePUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllResourceTypes_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceType"][];
                };
            };
        };
    };
    createResourceType_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourceTypePUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResourceType: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceType"];
                };
            };
        };
    };
    updateResourceType: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourceTypePUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_4: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResourceTypeSubtypes: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description If true, sub resource types are fetched recursively */
                recursive?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceType"][];
                };
            };
        };
    };
    getAllResourceTypeTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllResourceTypeTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getResourceTypeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    createUpdateResourceTypeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TranslationPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteResourceTypeTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllResources: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourcePUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllResources_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createResource_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourcePUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResourcePage: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    searchResources: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description How many results to return per page */
                pageSize?: number;
                /** @description Which page to fetch */
                page?: number;
                /** @description Query to search names */
                query?: string;
                /** @description Ids to fetch for query */
                ids?: string[];
                /** @description ContentURIs to fetch for query */
                contentUris?: string[];
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getResource: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"];
                };
            };
        };
    };
    updateResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourcePOST"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_3: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    cloneResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                /**
                 * @description Id of resource to clone
                 * @example urn:resource:1
                 */
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResourcePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getResourceFull: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeWithParents"];
                };
            };
        };
    };
    getMetadata_2: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata_2: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getResourceTypes: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResourceTypeWithConnection"][];
                };
            };
        };
    };
    getAllResourceTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllResourceTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getResourceTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    createUpdateResourceTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TranslationPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteResourceTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllSubjectTopics: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SubjectTopic"][];
                };
            };
        };
    };
    createSubjectTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectTopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllSubjectTopics_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SubjectTopic"][];
                };
            };
        };
    };
    createSubjectTopic_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectTopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getSubjectTopicPage: {
        parameters: {
            query?: {
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getSubjectTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SubjectTopic"];
                };
            };
        };
    };
    updateSubjectTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectTopicPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteSubjectTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllSubjects: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createSubject: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectPUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllSubjects_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createSubject_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectPUT"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getSubjectPage: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    searchSubjects: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description How many results to return per page */
                pageSize?: number;
                /** @description Which page to fetch */
                page?: number;
                /** @description Query to search names */
                query?: string;
                /** @description Ids to fetch for query */
                ids?: string[];
                /** @description ContentURIs to fetch for query */
                contentUris?: string[];
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getSubject: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"];
                };
            };
        };
    };
    updateSubject: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubjectPOST"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_2: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getMetadata_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getSubjectChildren: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description If true, subtopics are fetched recursively */
                recursive?: boolean;
                /** @description Select by relevance. If not specified, all nodes will be returned. */
                relevance?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    getAllSubjectTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllSubjectTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getSubjectTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    createUpdateSubjectTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TranslationPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteSubjectTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getSubjectResources: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by resource type id(s). If not specified, resources of all types will be returned.Multiple ids may be separated with comma or the parameter may be repeated for each id. */
                type?: string[];
                /** @description Select by relevance. If not specified, all resources will be returned. */
                relevance?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                subjectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    getAllTopicResources: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicResource"][];
                };
            };
        };
    };
    createTopicResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicResourcePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopicResources_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicResource"][];
                };
            };
        };
    };
    createTopicResource_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicResourcePOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getTopicResourcePage: {
        parameters: {
            query: {
                /** @description The page to fetch */
                page: number;
                /** @description Size of page to fetch */
                pageSize: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getTopicResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicResource"];
                };
            };
        };
    };
    updateTopicResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicResourcePUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteTopicResource: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopicSubtopics: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicSubtopic"][];
                };
            };
        };
    };
    createTopicSubtopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicSubtopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopicSubtopics_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicSubtopic"][];
                };
            };
        };
    };
    createTopicSubtopic_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicSubtopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getTopicSubtopicPage: {
        parameters: {
            query?: {
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getTopicSubtopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TopicSubtopic"];
                };
            };
        };
    };
    updateTopicSubtopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicSubtopicPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteTopicSubtopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopics: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopics_1: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description Filter by contentUri */
                contentURI?: string;
                /** @description Filter by key and value */
                key?: string;
                /** @description Filter by key and value */
                value?: string;
                /** @description Filter contexts by visibility */
                isVisible?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"][];
                };
            };
        };
    };
    createTopic_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicPOST"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getTopicsPage: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description The page to fetch */
                page?: number;
                /** @description Size of page to fetch */
                pageSize?: number;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    searchTopics: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description How many results to return per page */
                pageSize?: number;
                /** @description Which page to fetch */
                page?: number;
                /** @description Query to search names */
                query?: string;
                /** @description Ids to fetch for query */
                ids?: string[];
                /** @description ContentURIs to fetch for query */
                contentUris?: string[];
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SearchResult"];
                };
            };
        };
    };
    getTopic: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Node"];
                };
            };
        };
    };
    updateTopic: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TopicPostPut"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllTopicConnections: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Connection"][];
                };
            };
        };
    };
    makeResourcesPrimary: {
        parameters: {
            query?: {
                /** @description If true, children are fetched recursively */
                recursive?: boolean;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": boolean;
                };
            };
        };
    };
    getMetadata: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    putMetadata: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MetadataPUT"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Metadata"];
                };
            };
        };
    };
    getTopicResources: {
        parameters: {
            query?: {
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
                /** @description If true, resources from subtopics are fetched recursively */
                recursive?: boolean;
                /** @description Select by resource type id(s). If not specified, resources of all types will be returned.Multiple ids may be separated with comma or the parameter may be repeated for each id. */
                type?: string[];
                /** @description Select by relevance. If not specified, all resources will be returned. */
                relevance?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    getTopicSubTopics: {
        parameters: {
            query?: {
                /** @description Select filters by subject id if filter list is empty. Used as alternative to specify filters. */
                subject?: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NodeChild"][];
                };
            };
        };
    };
    getAllTopicTranslations: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getAllTopicTranslations_1: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"][];
                };
            };
        };
    };
    getTopicTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Translation"];
                };
            };
        };
    };
    createUpdateTopicTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TranslationPUT"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteTopicTranslation: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
                /**
                 * @description ISO-639-1 language code
                 * @example nb
                 */
                language: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getTaxonomyPathForUrl: {
        parameters: {
            query: {
                /**
                 * @description url in old rig except 'https://'
                 * @example ndla.no/nb/node/142542?fag=52253
                 */
                url: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResolvedOldUrl"];
                };
            };
        };
    };
    putTaxonomyNodeAndSubjectForOldUrl: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UrlMapping"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    resolve: {
        parameters: {
            query: {
                path: string;
                language?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResolvedUrl"];
                };
            };
        };
    };
    getAllVersions: {
        parameters: {
            query?: {
                /**
                 * @description Version type
                 * @example PUBLISHED
                 */
                type?: components["schemas"]["VersionType"];
                /**
                 * @description Version hash
                 * @example ndla
                 */
                hash?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Version"][];
                };
            };
        };
    };
    createVersion: {
        parameters: {
            query?: {
                /** @description Base new version on version with this id */
                sourceId?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VersionPostPut"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getAllVersions_1: {
        parameters: {
            query?: {
                /**
                 * @description Version type
                 * @example PUBLISHED
                 */
                type?: components["schemas"]["VersionType"];
                /**
                 * @description Version hash
                 * @example ndla
                 */
                hash?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Version"][];
                };
            };
        };
    };
    createVersion_1: {
        parameters: {
            query?: {
                /** @description Base new version on version with this id */
                sourceId?: string;
            };
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VersionPostPut"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getVersion: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Version"];
                };
            };
        };
    };
    updateVersion: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VersionPostPut"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deleteEntity: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    publishVersion: {
        parameters: {
            query?: never;
            header?: {
                versionHash?: components["parameters"]["versionHash"];
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}
