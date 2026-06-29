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
        delete: operations["deleteEntity_2"];
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
    "/v1/node-resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_189"];
        /** @deprecated */
        put: operations["redirect_191"];
        /** @deprecated */
        post: operations["redirect_190"];
        /** @deprecated */
        delete: operations["redirect_192"];
        /** @deprecated */
        options: operations["redirect_195"];
        /** @deprecated */
        head: operations["redirect_194"];
        /** @deprecated */
        patch: operations["redirect_193"];
        trace?: never;
    };
    "/v1/node-resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_203"];
        /** @deprecated */
        put: operations["redirect_205"];
        /** @deprecated */
        post: operations["redirect_204"];
        /** @deprecated */
        delete: operations["redirect_206"];
        /** @deprecated */
        options: operations["redirect_209"];
        /** @deprecated */
        head: operations["redirect_208"];
        /** @deprecated */
        patch: operations["redirect_207"];
        trace?: never;
    };
    "/v1/node-resources/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_196"];
        /** @deprecated */
        put: operations["redirect_198"];
        /** @deprecated */
        post: operations["redirect_197"];
        /** @deprecated */
        delete: operations["redirect_199"];
        /** @deprecated */
        options: operations["redirect_202"];
        /** @deprecated */
        head: operations["redirect_201"];
        /** @deprecated */
        patch: operations["redirect_200"];
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
        delete: operations["deleteEntity_1"];
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
        post: operations["cloneResource"];
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
        put: operations["makeResourcesPrimary"];
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
        /**
         * Gets all translations for a single node
         * @deprecated
         */
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
        /**
         * Gets all translations for a single node
         * @deprecated
         */
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
        /**
         * Gets a single translation for a single node
         * @deprecated
         */
        get: operations["getNodeTranslation"];
        /**
         * Creates or updates a translation of a node
         * @deprecated
         */
        put: operations["createUpdateNodeTranslation"];
        post?: never;
        /**
         * Deletes a translation
         * @deprecated
         */
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
         * DEPRECATED: Use /v1/nodes?nodeType=RESOURCE&contentURI= instead
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
         * DEPRECATED: Use /v1/nodes?nodeType=TOPIC&contentURI= instead
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
        /**
         * Gets all relevanceTranslations for a single relevance
         * @deprecated
         */
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
        /**
         * Gets all relevanceTranslations for a single relevance
         * @deprecated
         */
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
        /**
         * Gets a single translation for a single relevance
         * @deprecated
         */
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
        /**
         * Gets all connections between resources and resource types
         * @deprecated
         */
        get: operations["getAllResourceResourceTypes"];
        put?: never;
        /**
         * Adds a resource type to a resource
         * @deprecated
         */
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
        /**
         * Gets all connections between resources and resource types
         * @deprecated
         */
        get: operations["getAllResourceResourceTypes_1"];
        put?: never;
        /**
         * Adds a resource type to a resource
         * @deprecated
         */
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
        /**
         * Gets a single connection between resource and resource type
         * @deprecated
         */
        get: operations["getResourceResourceType"];
        put?: never;
        post?: never;
        /**
         * Removes a resource type from a resource
         * @deprecated
         */
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
        post?: never;
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
        post?: never;
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
        put?: never;
        post?: never;
        delete?: never;
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
        /**
         * Gets all relevanceTranslations for a single resource type
         * @deprecated
         */
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
        /**
         * Gets all relevanceTranslations for a single resource type
         * @deprecated
         */
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
        /**
         * Gets a single translation for a single resource type
         * @deprecated
         */
        get: operations["getResourceTypeTranslation"];
        put?: never;
        post?: never;
        delete?: never;
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
        /** @deprecated */
        get: operations["redirect_168"];
        /** @deprecated */
        put: operations["redirect_170"];
        /** @deprecated */
        post: operations["redirect_169"];
        /** @deprecated */
        delete: operations["redirect_171"];
        /** @deprecated */
        options: operations["redirect_174"];
        /** @deprecated */
        head: operations["redirect_173"];
        /** @deprecated */
        patch: operations["redirect_172"];
        trace?: never;
    };
    "/v1/resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_182"];
        /** @deprecated */
        put: operations["redirect_184"];
        /** @deprecated */
        post: operations["redirect_183"];
        /** @deprecated */
        delete: operations["redirect_185"];
        /** @deprecated */
        options: operations["redirect_188"];
        /** @deprecated */
        head: operations["redirect_187"];
        /** @deprecated */
        patch: operations["redirect_186"];
        trace?: never;
    };
    "/v1/resources/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_175"];
        /** @deprecated */
        put: operations["redirect_177"];
        /** @deprecated */
        post: operations["redirect_176"];
        /** @deprecated */
        delete: operations["redirect_178"];
        /** @deprecated */
        options: operations["redirect_181"];
        /** @deprecated */
        head: operations["redirect_180"];
        /** @deprecated */
        patch: operations["redirect_179"];
        trace?: never;
    };
    "/v1/resources/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_147"];
        /** @deprecated */
        put: operations["redirect_149"];
        /** @deprecated */
        post: operations["redirect_148"];
        /** @deprecated */
        delete: operations["redirect_150"];
        /** @deprecated */
        options: operations["redirect_153"];
        /** @deprecated */
        head: operations["redirect_152"];
        /** @deprecated */
        patch: operations["redirect_151"];
        trace?: never;
    };
    "/v1/resources/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_161"];
        /** @deprecated */
        put: operations["redirect_163"];
        /** @deprecated */
        post: operations["redirect_162"];
        /** @deprecated */
        delete: operations["redirect_164"];
        /** @deprecated */
        options: operations["redirect_167"];
        /** @deprecated */
        head: operations["redirect_166"];
        /** @deprecated */
        patch: operations["redirect_165"];
        trace?: never;
    };
    "/v1/resources/{id}/translations/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_154"];
        /** @deprecated */
        put: operations["redirect_156"];
        /** @deprecated */
        post: operations["redirect_155"];
        /** @deprecated */
        delete: operations["redirect_157"];
        /** @deprecated */
        options: operations["redirect_160"];
        /** @deprecated */
        head: operations["redirect_159"];
        /** @deprecated */
        patch: operations["redirect_158"];
        trace?: never;
    };
    "/v1/subject-topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_126"];
        /** @deprecated */
        put: operations["redirect_128"];
        /** @deprecated */
        post: operations["redirect_127"];
        /** @deprecated */
        delete: operations["redirect_129"];
        /** @deprecated */
        options: operations["redirect_132"];
        /** @deprecated */
        head: operations["redirect_131"];
        /** @deprecated */
        patch: operations["redirect_130"];
        trace?: never;
    };
    "/v1/subject-topics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_140"];
        /** @deprecated */
        put: operations["redirect_142"];
        /** @deprecated */
        post: operations["redirect_141"];
        /** @deprecated */
        delete: operations["redirect_143"];
        /** @deprecated */
        options: operations["redirect_146"];
        /** @deprecated */
        head: operations["redirect_145"];
        /** @deprecated */
        patch: operations["redirect_144"];
        trace?: never;
    };
    "/v1/subject-topics/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_133"];
        /** @deprecated */
        put: operations["redirect_135"];
        /** @deprecated */
        post: operations["redirect_134"];
        /** @deprecated */
        delete: operations["redirect_136"];
        /** @deprecated */
        options: operations["redirect_139"];
        /** @deprecated */
        head: operations["redirect_138"];
        /** @deprecated */
        patch: operations["redirect_137"];
        trace?: never;
    };
    "/v1/subjects": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_105"];
        /** @deprecated */
        put: operations["redirect_107"];
        /** @deprecated */
        post: operations["redirect_106"];
        /** @deprecated */
        delete: operations["redirect_108"];
        /** @deprecated */
        options: operations["redirect_111"];
        /** @deprecated */
        head: operations["redirect_110"];
        /** @deprecated */
        patch: operations["redirect_109"];
        trace?: never;
    };
    "/v1/subjects/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_119"];
        /** @deprecated */
        put: operations["redirect_121"];
        /** @deprecated */
        post: operations["redirect_120"];
        /** @deprecated */
        delete: operations["redirect_122"];
        /** @deprecated */
        options: operations["redirect_125"];
        /** @deprecated */
        head: operations["redirect_124"];
        /** @deprecated */
        patch: operations["redirect_123"];
        trace?: never;
    };
    "/v1/subjects/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_112"];
        /** @deprecated */
        put: operations["redirect_114"];
        /** @deprecated */
        post: operations["redirect_113"];
        /** @deprecated */
        delete: operations["redirect_115"];
        /** @deprecated */
        options: operations["redirect_118"];
        /** @deprecated */
        head: operations["redirect_117"];
        /** @deprecated */
        patch: operations["redirect_116"];
        trace?: never;
    };
    "/v1/subjects/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_84"];
        /** @deprecated */
        put: operations["redirect_86"];
        /** @deprecated */
        post: operations["redirect_85"];
        /** @deprecated */
        delete: operations["redirect_87"];
        /** @deprecated */
        options: operations["redirect_90"];
        /** @deprecated */
        head: operations["redirect_89"];
        /** @deprecated */
        patch: operations["redirect_88"];
        trace?: never;
    };
    "/v1/subjects/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_98"];
        /** @deprecated */
        put: operations["redirect_100"];
        /** @deprecated */
        post: operations["redirect_99"];
        /** @deprecated */
        delete: operations["redirect_101"];
        /** @deprecated */
        options: operations["redirect_104"];
        /** @deprecated */
        head: operations["redirect_103"];
        /** @deprecated */
        patch: operations["redirect_102"];
        trace?: never;
    };
    "/v1/subjects/{id}/translations/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_91"];
        /** @deprecated */
        put: operations["redirect_93"];
        /** @deprecated */
        post: operations["redirect_92"];
        /** @deprecated */
        delete: operations["redirect_94"];
        /** @deprecated */
        options: operations["redirect_97"];
        /** @deprecated */
        head: operations["redirect_96"];
        /** @deprecated */
        patch: operations["redirect_95"];
        trace?: never;
    };
    "/v1/topic-resources": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_63"];
        /** @deprecated */
        put: operations["redirect_65"];
        /** @deprecated */
        post: operations["redirect_64"];
        /** @deprecated */
        delete: operations["redirect_66"];
        /** @deprecated */
        options: operations["redirect_69"];
        /** @deprecated */
        head: operations["redirect_68"];
        /** @deprecated */
        patch: operations["redirect_67"];
        trace?: never;
    };
    "/v1/topic-resources/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_77"];
        /** @deprecated */
        put: operations["redirect_79"];
        /** @deprecated */
        post: operations["redirect_78"];
        /** @deprecated */
        delete: operations["redirect_80"];
        /** @deprecated */
        options: operations["redirect_83"];
        /** @deprecated */
        head: operations["redirect_82"];
        /** @deprecated */
        patch: operations["redirect_81"];
        trace?: never;
    };
    "/v1/topic-resources/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_70"];
        /** @deprecated */
        put: operations["redirect_72"];
        /** @deprecated */
        post: operations["redirect_71"];
        /** @deprecated */
        delete: operations["redirect_73"];
        /** @deprecated */
        options: operations["redirect_76"];
        /** @deprecated */
        head: operations["redirect_75"];
        /** @deprecated */
        patch: operations["redirect_74"];
        trace?: never;
    };
    "/v1/topic-subtopics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_42"];
        /** @deprecated */
        put: operations["redirect_44"];
        /** @deprecated */
        post: operations["redirect_43"];
        /** @deprecated */
        delete: operations["redirect_45"];
        /** @deprecated */
        options: operations["redirect_48"];
        /** @deprecated */
        head: operations["redirect_47"];
        /** @deprecated */
        patch: operations["redirect_46"];
        trace?: never;
    };
    "/v1/topic-subtopics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_56"];
        /** @deprecated */
        put: operations["redirect_58"];
        /** @deprecated */
        post: operations["redirect_57"];
        /** @deprecated */
        delete: operations["redirect_59"];
        /** @deprecated */
        options: operations["redirect_62"];
        /** @deprecated */
        head: operations["redirect_61"];
        /** @deprecated */
        patch: operations["redirect_60"];
        trace?: never;
    };
    "/v1/topic-subtopics/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_49"];
        /** @deprecated */
        put: operations["redirect_51"];
        /** @deprecated */
        post: operations["redirect_50"];
        /** @deprecated */
        delete: operations["redirect_52"];
        /** @deprecated */
        options: operations["redirect_55"];
        /** @deprecated */
        head: operations["redirect_54"];
        /** @deprecated */
        patch: operations["redirect_53"];
        trace?: never;
    };
    "/v1/topics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_21"];
        /** @deprecated */
        put: operations["redirect_23"];
        /** @deprecated */
        post: operations["redirect_22"];
        /** @deprecated */
        delete: operations["redirect_24"];
        /** @deprecated */
        options: operations["redirect_27"];
        /** @deprecated */
        head: operations["redirect_26"];
        /** @deprecated */
        patch: operations["redirect_25"];
        trace?: never;
    };
    "/v1/topics/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_35"];
        /** @deprecated */
        put: operations["redirect_37"];
        /** @deprecated */
        post: operations["redirect_36"];
        /** @deprecated */
        delete: operations["redirect_38"];
        /** @deprecated */
        options: operations["redirect_41"];
        /** @deprecated */
        head: operations["redirect_40"];
        /** @deprecated */
        patch: operations["redirect_39"];
        trace?: never;
    };
    "/v1/topics/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_28"];
        /** @deprecated */
        put: operations["redirect_30"];
        /** @deprecated */
        post: operations["redirect_29"];
        /** @deprecated */
        delete: operations["redirect_31"];
        /** @deprecated */
        options: operations["redirect_34"];
        /** @deprecated */
        head: operations["redirect_33"];
        /** @deprecated */
        patch: operations["redirect_32"];
        trace?: never;
    };
    "/v1/topics/{id}/translations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect"];
        /** @deprecated */
        put: operations["redirect_2"];
        /** @deprecated */
        post: operations["redirect_1"];
        /** @deprecated */
        delete: operations["redirect_3"];
        /** @deprecated */
        options: operations["redirect_6"];
        /** @deprecated */
        head: operations["redirect_5"];
        /** @deprecated */
        patch: operations["redirect_4"];
        trace?: never;
    };
    "/v1/topics/{id}/translations/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_14"];
        /** @deprecated */
        put: operations["redirect_16"];
        /** @deprecated */
        post: operations["redirect_15"];
        /** @deprecated */
        delete: operations["redirect_17"];
        /** @deprecated */
        options: operations["redirect_20"];
        /** @deprecated */
        head: operations["redirect_19"];
        /** @deprecated */
        patch: operations["redirect_18"];
        trace?: never;
    };
    "/v1/topics/{id}/translations/**": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @deprecated */
        get: operations["redirect_7"];
        /** @deprecated */
        put: operations["redirect_9"];
        /** @deprecated */
        post: operations["redirect_8"];
        /** @deprecated */
        delete: operations["redirect_10"];
        /** @deprecated */
        options: operations["redirect_13"];
        /** @deprecated */
        head: operations["redirect_12"];
        /** @deprecated */
        patch: operations["redirect_11"];
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
            path?: string | null;
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
            } | null;
            /** @description Set of grep codes, Only updated if present */
            grepCodes?: string[] | null;
            /** @description Visibility of the node, Only updated if present */
            visible?: boolean | null;
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
            /**
             * Format: date-time
             * @description When was this last updated.
             */
            updatedAt: string;
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
            /**
             * Format: date-time
             * @description When was this last updated.
             */
            updatedAt: string;
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
            /** @description ResourceType public ids to assign to the node. Only works for nodes of type RESOURCE */
            resourceTypes?: string[];
            /**
             * @deprecated
             * @description The node is a root node. Default is false. Only used if present.
             */
            root?: boolean;
            /** @description The technical evaluation of the node. Contains a flag and an optional comment. Can be null to remove existing evaluation. */
            technicalEvaluation?: components["schemas"]["TechnicalEvaluationDTO"];
            /** @description The translations for the node. Contains an array of translations in different languages */
            translations?: components["schemas"]["Translation"][];
            /** @description The node is visible. Default is true. */
            visible?: boolean;
        };
        NodeSearchBody: {
            /** @description ContentURIs to fetch for query */
            contentUris?: string[] | null;
            /** @description If specified, the search result will be filtered by whether they include the key,value combination provided. If more than one provided only one will be required (OR) */
            customFields?: {
                [key: string]: string;
            } | null;
            /** @description Filter out programme contexts */
            filterProgrammes: boolean;
            /** @description Ids to fetch for query */
            ids?: string[] | null;
            /** @description Include all contexts */
            includeContexts: boolean;
            /**
             * @description ISO-639-1 language code
             * @example nb
             */
            language: string;
            /** @description Filter by nodeType */
            nodeType?: components["schemas"]["NodeType"][] | null;
            /**
             * Format: int32
             * @description Which page to fetch
             */
            page: number;
            /**
             * Format: int32
             * @description How many results to return per page
             */
            pageSize: number;
            /**
             * Format: uri
             * @description Id to parent id in context.
             */
            parentId?: string | null;
            /** @description Query to search names */
            query?: string | null;
            /**
             * Format: uri
             * @description Id to root id in context.
             */
            rootId?: string | null;
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
            /**
             * Format: date-time
             * @description When was this last updated.
             */
            updatedAt?: string;
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
            contentUri?: string | null;
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
        ResourceResourceType: {
            /**
             * Format: uri
             * @description Resource to resource type connection id
             * @example urn:resource-resourcetype:urn:resource:123_urn:resourcetype:subjectMaterial
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
            order: number;
            /** @description Sub resource types */
            subtypes?: components["schemas"]["ResourceType"][] | null;
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description All translations of this resource type */
            translations: components["schemas"]["Translation"][];
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
            order: number;
            /**
             * Format: uri
             * @example urn:resourcetype:1
             */
            parentId?: string | null;
            /** @description List of language codes supported by translations */
            supportedLanguages: string[];
            /** @description All translations of this resource type */
            translations: components["schemas"]["Translation"][];
        };
        SearchResult: {
            /**
             * Format: int32
             * @description The page number
             */
            page: number;
            /**
             * Format: int32
             * @description The page size
             */
            pageSize: number;
            /** @description List of search results */
            results: components["schemas"]["Node"][];
            /**
             * Format: int64
             * @description Total search result count, useful for fetching multiple pages
             */
            totalCount: number;
        };
        SearchableTaxonomyResourceType: {
            id: string;
            name: {
                [key: string]: string;
            };
            /** Format: int32 */
            order: number;
            parentId?: string | null;
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
            subjectId?: string | null;
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
            archived?: string | null;
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
            published?: string | null;
            /** @example BETA */
            versionType: components["schemas"]["VersionType"];
        };
        /** @description The new version */
        VersionPost: {
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:version: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:version:1
             */
            id?: string | null;
            /** @description If specified, set the locked property to this value */
            locked?: boolean | null;
            /**
             * @description The name of the version
             * @example Beta 2022
             */
            name: string;
        };
        /** @description The updated version. */
        VersionPut: {
            /**
             * Format: uri
             * @description If specified, set the id to this value. Must start with urn:version: and be a valid URI. If omitted, an id will be assigned automatically.
             * @example urn:version:1
             */
            id?: string | null;
            /** @description If specified, set the locked property to this value */
            locked?: boolean | null;
            /**
             * @description If specified, set the name to this value.
             * @example Beta 2022
             */
            name?: string | null;
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
export type NodeSearchBody = components['schemas']['NodeSearchBody'];
export type NodeType = components['schemas']['NodeType'];
export type NodeWithParents = components['schemas']['NodeWithParents'];
export type QualityEvaluationDTO = components['schemas']['QualityEvaluationDTO'];
export type Relevance = components['schemas']['Relevance'];
export type ResolvedOldUrl = components['schemas']['ResolvedOldUrl'];
export type ResolvedUrl = components['schemas']['ResolvedUrl'];
export type ResourceResourceType = components['schemas']['ResourceResourceType'];
export type ResourceResourceTypePOST = components['schemas']['ResourceResourceTypePOST'];
export type ResourceType = components['schemas']['ResourceType'];
export type ResourceTypeWithConnection = components['schemas']['ResourceTypeWithConnection'];
export type SearchResult = components['schemas']['SearchResult'];
export type SearchableTaxonomyResourceType = components['schemas']['SearchableTaxonomyResourceType'];
export type TaxonomyContext = components['schemas']['TaxonomyContext'];
export type TaxonomyCrumb = components['schemas']['TaxonomyCrumb'];
export type TechnicalEvaluationDTO = components['schemas']['TechnicalEvaluationDTO'];
export type Translation = components['schemas']['Translation'];
export type TranslationPUT = components['schemas']['TranslationPUT'];
export type UrlMapping = components['schemas']['UrlMapping'];
export type Version = components['schemas']['Version'];
export type VersionPost = components['schemas']['VersionPost'];
export type VersionPut = components['schemas']['VersionPut'];
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
    redirect_189: {
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
    redirect_191: {
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
    redirect_190: {
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
    redirect_192: {
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
    redirect_195: {
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
    redirect_194: {
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
    redirect_193: {
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
    redirect_203: {
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
    redirect_205: {
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
    redirect_204: {
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
    redirect_206: {
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
    redirect_209: {
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
    redirect_208: {
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
    redirect_207: {
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
    redirect_196: {
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
    redirect_198: {
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
    redirect_197: {
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
    redirect_199: {
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
    redirect_202: {
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
    redirect_201: {
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
    redirect_200: {
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
                /** @description Filter by context ids. Beware: handled separately from other parameters! */
                contextIds?: string[];
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
                /** @description Filter by context ids. Beware: handled separately from other parameters! */
                contextIds?: string[];
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
    cloneResource: {
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
    queryTopics: {
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
    redirect_168: {
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
    redirect_170: {
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
    redirect_169: {
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
    redirect_171: {
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
    redirect_174: {
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
    redirect_173: {
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
    redirect_172: {
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
    redirect_182: {
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
    redirect_184: {
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
    redirect_183: {
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
    redirect_185: {
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
    redirect_188: {
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
    redirect_187: {
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
    redirect_186: {
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
    redirect_175: {
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
    redirect_177: {
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
    redirect_176: {
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
    redirect_178: {
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
    redirect_181: {
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
    redirect_180: {
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
    redirect_179: {
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
    redirect_147: {
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
    redirect_149: {
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
    redirect_148: {
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
    redirect_150: {
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
    redirect_153: {
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
    redirect_152: {
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
    redirect_151: {
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
    redirect_161: {
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
    redirect_163: {
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
    redirect_162: {
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
    redirect_164: {
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
    redirect_167: {
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
    redirect_166: {
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
    redirect_165: {
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
    redirect_154: {
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
    redirect_156: {
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
    redirect_155: {
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
    redirect_157: {
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
    redirect_160: {
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
    redirect_159: {
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
    redirect_158: {
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
    redirect_126: {
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
    redirect_128: {
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
    redirect_127: {
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
    redirect_129: {
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
    redirect_132: {
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
    redirect_131: {
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
    redirect_130: {
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
    redirect_140: {
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
    redirect_142: {
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
    redirect_141: {
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
    redirect_143: {
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
    redirect_146: {
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
    redirect_145: {
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
    redirect_144: {
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
    redirect_133: {
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
    redirect_135: {
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
    redirect_134: {
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
    redirect_136: {
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
    redirect_139: {
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
    redirect_138: {
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
    redirect_137: {
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
    redirect_105: {
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
    redirect_107: {
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
    redirect_106: {
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
    redirect_108: {
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
    redirect_111: {
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
    redirect_110: {
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
    redirect_109: {
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
    redirect_119: {
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
    redirect_121: {
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
    redirect_120: {
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
    redirect_122: {
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
    redirect_125: {
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
    redirect_124: {
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
    redirect_123: {
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
    redirect_112: {
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
    redirect_114: {
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
    redirect_113: {
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
    redirect_115: {
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
    redirect_118: {
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
    redirect_117: {
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
    redirect_116: {
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
    redirect_84: {
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
    redirect_86: {
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
    redirect_85: {
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
    redirect_87: {
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
    redirect_90: {
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
    redirect_89: {
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
    redirect_88: {
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
    redirect_98: {
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
    redirect_100: {
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
    redirect_99: {
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
    redirect_101: {
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
    redirect_104: {
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
    redirect_103: {
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
    redirect_102: {
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
    redirect_91: {
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
    redirect_93: {
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
    redirect_92: {
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
    redirect_94: {
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
    redirect_97: {
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
    redirect_96: {
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
    redirect_95: {
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
    redirect_63: {
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
    redirect_65: {
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
    redirect_64: {
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
    redirect_66: {
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
    redirect_69: {
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
    redirect_68: {
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
    redirect_67: {
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
    redirect_77: {
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
    redirect_79: {
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
    redirect_78: {
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
    redirect_80: {
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
    redirect_83: {
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
    redirect_82: {
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
    redirect_81: {
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
    redirect_70: {
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
    redirect_72: {
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
    redirect_71: {
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
    redirect_73: {
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
    redirect_76: {
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
    redirect_75: {
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
    redirect_74: {
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
    redirect_42: {
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
    redirect_44: {
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
    redirect_43: {
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
    redirect_45: {
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
    redirect_48: {
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
    redirect_47: {
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
    redirect_46: {
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
    redirect_56: {
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
    redirect_58: {
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
    redirect_57: {
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
    redirect_59: {
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
    redirect_62: {
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
    redirect_61: {
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
    redirect_60: {
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
    redirect_49: {
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
    redirect_51: {
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
    redirect_50: {
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
    redirect_52: {
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
    redirect_55: {
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
    redirect_54: {
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
    redirect_53: {
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
    redirect_21: {
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
    redirect_23: {
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
    redirect_22: {
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
    redirect_24: {
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
    redirect_27: {
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
    redirect_26: {
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
    redirect_25: {
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
    redirect_35: {
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
    redirect_37: {
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
    redirect_36: {
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
    redirect_38: {
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
    redirect_41: {
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
    redirect_40: {
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
    redirect_39: {
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
    redirect_28: {
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
    redirect_30: {
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
    redirect_29: {
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
    redirect_31: {
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
    redirect_34: {
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
    redirect_33: {
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
    redirect_32: {
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
    redirect: {
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
    redirect_2: {
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
    redirect_1: {
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
    redirect_3: {
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
    redirect_6: {
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
    redirect_5: {
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
    redirect_4: {
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
    redirect_14: {
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
    redirect_16: {
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
    redirect_15: {
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
    redirect_17: {
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
    redirect_20: {
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
    redirect_19: {
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
    redirect_18: {
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
    redirect_7: {
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
    redirect_9: {
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
    redirect_8: {
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
    redirect_10: {
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
    redirect_13: {
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
    redirect_12: {
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
    redirect_11: {
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
                "application/json": components["schemas"]["VersionPost"];
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
                "application/json": components["schemas"]["VersionPost"];
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
                "application/json": components["schemas"]["VersionPut"];
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
