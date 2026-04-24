/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.security;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Profile("auth")
@Component
@Order(2)
public class AuthFilter extends GenericFilterBean {

    @Value(value = "${auth0.audience}")
    private String audience;

    @Value(value = "${auth0.issuer}")
    private String issuer;

    @Value(value = "${auth0.jwks.kid}")
    private String kid;

    private RSAPublicKey publicKey;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    @EventListener(ApplicationReadyEvent.class)
    public void initializePublicKeyOnApplicationStartup() {
        JwkProvider provider = new UrlJwkProvider(issuer);
        try {
            Jwk jwk = provider.get(kid);
            publicKey = (RSAPublicKey) jwk.getPublicKey();
        } catch (JwkException ex) {
            LOGGER.error("Failed to fetch public key from " + issuer, ex);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        parseWebToken((HttpServletRequest) servletRequest);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void parseWebToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("authorization");
        if (!isBlank(authorizationHeader) && (authorizationHeader.startsWith("Bearer"))) {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JWTAuthentication(
                            verifyWebToken(authorizationHeader.substring(6).trim())));
        }
    }

    private DecodedJWT verifyWebToken(String token) {
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier =
                JWT.require(algorithm).withAnyOfAudience(audience).build();
        return verifier.verify(token);
    }
}
