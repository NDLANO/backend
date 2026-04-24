/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.Serial;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JWTAuthentication implements Authentication {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String TAXONOMY_API = "taxonomy";
    private static final String WRITE_PERMISSION = "write";
    private static final String ADMIN_PERMISSION = "admin";

    private final Collection<GrantedAuthority> authorities;
    private boolean authenticated;
    private final Map<String, Claim> claims;
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthentication.class);

    public JWTAuthentication(DecodedJWT token) {
        Claim scope = token.getClaim("scope");
        Claim permissions = token.getClaim("permissions");

        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("READONLY"));

        try {
            if (permissions != null && permissions.asArray(String.class) != null) {
                auths.addAll(addPermissionsAsAuthorities(permissions.asArray(String.class)));
            } else if (scope != null && scope.asString() != null) {
                auths.addAll(addPermissionsAsAuthorities(scope.asString().split(" ")));
            }
        } catch (Exception e) {
            LOGGER.error("JWT Authentication failed", e);
        }

        this.authorities = Collections.unmodifiableList(auths);
        this.claims = token.getClaims();
        authenticated = true;
    }

    private List<GrantedAuthority> addPermissionsAsAuthorities(String[] allPermissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String jwtPermissionString : allPermissions) {
            final JWTPermission jwtPermission = new JWTPermission(jwtPermissionString);
            if (jwtPermission.getApi() != null
                    && jwtPermission.getPermission() != null
                    && TAXONOMY_API.equals(jwtPermission.getApi())) {
                if (WRITE_PERMISSION.equals(jwtPermission.getPermission()))
                    authorities.add(new SimpleGrantedAuthority("TAXONOMY_WRITE"));
                if (ADMIN_PERMISSION.equals(jwtPermission.getPermission()))
                    authorities.add(new SimpleGrantedAuthority("TAXONOMY_ADMIN"));
            }
        }
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return new Object();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getDetails() {
        return claims;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }
}
