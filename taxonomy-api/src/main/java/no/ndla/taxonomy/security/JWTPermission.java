/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.security;

public class JWTPermission {
    private final String api;
    private final String environment;
    private final String permission;

    public JWTPermission(String scope) {
        String parts[] = scope.split(":");
        if (parts.length > 1) {
            if (parts[0].contains("-")) {
                this.api = parts[0].split("-")[0].trim();
                this.environment = parts[0].split("-")[1].split(":")[0].trim();
                this.permission = parts[1].trim();
            } else {
                this.api = parts[0].trim();
                this.environment = null;
                this.permission = parts[1].trim();
            }
        } else {
            this.api = null;
            this.environment = null;
            this.permission = null;
        }
    }

    public String getApi() {
        return api;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getPermission() {
        return permission;
    }
}
