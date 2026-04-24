/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import no.ndla.taxonomy.security.JWTAuthentication;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class JWTTokenTest {

    String auth =
            "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoicjBnSGI5WGczbGk0eXlYdjBRU0dRY3pWM2J2aWFrclQiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6InIwZ0hiOVhnM2xpNHl5WHYwUVNHUWN6VjNidmlha3JUQGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxNTQ5NTUzMCwiZXhwIjoxNTE1NTgxOTMwLCJzY29wZSI6ImRyYWZ0cy1zdGFnaW5nOndyaXRlIGFydGljbGVzLXRlc3Q6cHVibGlzaCBpbWFnZXMtcHJvZDp3cml0ZSBsaXN0aW5nLXRlc3Q6d3JpdGUgYXJ0aWNsZXMtYnJ1a2VydGVzdDp3cml0ZSBsaXN0aW5nLXByb2Q6d3JpdGUgYXJ0aWNsZXMtdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOndyaXRlIGxpc3RpbmctYnJ1a2VydGVzdDp3cml0ZSBpbWFnZXMtc3RhZ2luZzp3cml0ZSBpbWFnZXMtdGVzdDp3cml0ZSBhcnRpY2xlcy1wcm9kOnB1Ymxpc2ggYXJ0aWNsZXMtYnJ1a2VydGVzdDpwdWJsaXNoIGRyYWZ0cy10ZXN0OnNldF90b19wdWJsaXNoIGxpc3Rpbmctc3RhZ2luZzp3cml0ZSBhdWRpby1icnVrZXJ0ZXN0OndyaXRlIGRyYWZ0cy1wcm9kOndyaXRlIGltYWdlcy1icnVrZXJ0ZXN0OndyaXRlIGRyYWZ0cy10ZXN0OndyaXRlIGF1ZGlvLXByb2Q6d3JpdGUgYXVkaW8tdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOnB1Ymxpc2ggYXJ0aWNsZXMtcHJvZDp3cml0ZSBkcmFmdHMtc3RhZ2luZzpzZXRfdG9fcHVibGlzaCBhdWRpby1zdGFnaW5nOndyaXRlIGRyYWZ0cy1icnVrZXJ0ZXN0OnNldF90b19wdWJsaXNoIGRyYWZ0cy1wcm9kOnNldF90b19wdWJsaXNoIGRyYWZ0cy1icnVrZXJ0ZXN0OndyaXRlIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.cOhrV1JshGYyD-CI-jh_omwy_ehDZJFq1P3C3FibhgcG2V3q6emPftmyOfKstp5_HxBWfULeq4p3Du5GlpJ5oADGSES2KlfSAZFQQWoN2auCqL4TP7lR7syosEftwWa6XIYMgzqk5r4KH4F0ay0v5n7_sU99r6HDDi8bsuLpgoXgcRwtx_SIh_CJWplS30Iiz8EdITdvknU40_t8zeqp7CoYnnZRQtm79qHxMGXyyiSqGPISUkzEK6O8HPNh0dVWvyI-SNzAQyJQg8J3RC26bgo7SpVDt8oGGMiKsBnssuXO60MqEs_vt3Q4-xdXIJQOj0YX4ICEeHJ3nq1pigWRLg";
    String authV2 =
            "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiMjRaYmkyYjJycGdVVnZQNzFEV3ZTNUloVVk0ZlpGSjNAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTQ0NTIwMjg3LCJleHAiOjE1NDQ1MjIwODcsImF6cCI6IjI0WmJpMmIycnBnVVZ2UDcxRFd2UzVJaFVZNGZaRkozIiwic2NvcGUiOiJ0YXhvbm9teTp3cml0ZSIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Yon-Jm1RAe5jn0bS8NTQ2bIZWNAO-M3JN_G8PvxbuMtKLqOZ7SydAx9cgDysKg95HRj8tCVpj1fDttKSIm59Ww-400gmDTTlQPqdzlNWsXT7wgAllTboXEigGAmjj1tNKYFQxTdIa9azdiqUPT-e7ao5JOdM3_uYFiINC_MoxfFaezGSWeja4TkEakj8MyxGraKa6DhXrlLd1TfZtH6bv6mxEyAy_YmYZQ1vQ8iZKdGM1AHp703yoqITutrfzdZOx-yxNUcmPJwqzcm3cXd0RA-qwJ5NfsuZrS52NtrDqRaxGdIuai9W04nM5ZEfvkvpFGe8gU6fZ8is0ThiS39luQ";
    String authV2WithAdmin =
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZnNleE9DZkpGR09LdXkxQzJlNzFPc3ZRd3EwTldLQUtAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNjM2NTQ0MDIxLCJleHAiOjE2MzY1NDc2MjEsImF6cCI6ImZzZXhPQ2ZKRkdPS3V5MUMyZTcxT3N2UXdxME5XS0FLIiwic2NvcGUiOiJhcnRpY2xlczpwdWJsaXNoIGFydGljbGVzOndyaXRlIGF1ZGlvOndyaXRlIGNvbmNlcHQ6d3JpdGUgZHJhZnRzOmFkbWluIGRyYWZ0czpwdWJsaXNoIGRyYWZ0czp3cml0ZSBpbWFnZXM6d3JpdGUgbGVhcm5pbmdwYXRoOmFkbWluIGxlYXJuaW5ncGF0aDpwdWJsaXNoIGxlYXJuaW5ncGF0aDp3cml0ZSB0YXhvbm9teTphZG1pbiB0YXhvbm9teTp3cml0ZSIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.KFJ1HF1b3otp2WIKVDh9It_OwnWY6WkibcgxWJkyFfHFZERtgxRUCHVUKPxoQds4ooYjIv9mwx0jZNAjA0v2J6d8FB5sfKoEPTt4zO7TTSBkehytKgWawqtT_bBWoxYralw__hSWE2-q5UMbMN_i-Z09_8M9SojaWVF1cuTdLf4XBa24cl4oj4IfIQdGKaqI1qdXS8fwn0EoID419a3u-w_sg_m28R2lqdot-6gePeZZJE6dpTeiJC7rGDL9YDJ4JxnI4ZmAXvYMeResJJBFo_Ur5aqeXHZqr34y1LdywIiIWp7I0CMPk6T9HD_GxCLVmFk87zokxUT4Shx_Pa8y5A";
    String authV2WithPermissions =
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inlhc2QiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiVGVzdCBCcnVrZXIiLCJodHRwczovL25kbGEubm8vdXNlcl9lbWFpbCI6InRlc3RAbmRsYS5ubyIsImlzcyI6Imh0dHBzOi8vbmRsYS10ZXN0LmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJzdWIiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTYzODg3OTM5NCwiZXhwIjoxNjM4ODgyOTk0LCJhenAiOiJGSzM1RkQzWUhPZWFZY1hHODBFVkNiQm1BZmlGR3ppViIsInNjb3BlIjoib3BlbmlkIGVtYWlsIiwicGVybWlzc2lvbnMiOlsidGF4b25vbXk6YWRtaW4iLCJ0YXhvbm9teTp3cml0ZSJdfQ.Tyf84pzpIpTs6vJKkIv_qDZPbYtjcZ7_0eeQl3c3JCls6y99vlZjCnMoBg9xSRstw1dvwbnqSrEPx5HDTWlJUegLwtApi4Xzv08bAE669jv_oJvZDRLJRnxcIHE6kRItmgI1jRcPs5wLcukkeEjCduTVjLeKXhbiLRv0OsWmz1nT7elMQ_yBltDkI725NPeH4RbVgr6u_ag8GlBEoJiQTYgW9II3eUdQvTh7PzxCb_hkktEvVjAn_vDIEs_2AqaPd2uAOpnkDZPujvfbBQFd75aJOuwwEKa06qECDTHG1z4F-mtN0IzVOBmh7i0rp7ovPmJEUhdb36--6jwstWYzpQ";

    @Test
    public void no_taxonomy_in_old_token_gives_read_permission() {
        DecodedJWT jwt = JWT.decode(auth.substring(6).trim());

        JWTAuthentication token = new JWTAuthentication(jwt);
        assertTrue(token.isAuthenticated());
        assertEquals(1, token.getAuthorities().size());
    }

    @Test
    public void jwt_token_gives_read_write_permissions() {
        DecodedJWT jwt = JWT.decode(authV2.substring(6).trim());
        JWTAuthentication token = new JWTAuthentication(jwt);
        assertTrue(token.isAuthenticated());
        assertEquals(2, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "READONLY".equals(au.getAuthority()));
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_WRITE".equals(au.getAuthority()));
    }

    @Test
    public void jwt_token_gives_read_write_admin_permissions() {
        DecodedJWT jwt = JWT.decode(authV2WithAdmin.substring(6).trim());
        JWTAuthentication token = new JWTAuthentication(jwt);
        assertTrue(token.isAuthenticated());
        assertEquals(3, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "READONLY".equals(au.getAuthority()));
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_ADMIN".equals(au.getAuthority()));
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_WRITE".equals(au.getAuthority()));
    }

    @Test
    public void jwt_token_with_permissions_gives_read_write_admin_permissions() {
        DecodedJWT jwt = JWT.decode(authV2WithPermissions.substring(6).trim());
        JWTAuthentication token = new JWTAuthentication(jwt);
        assertTrue(token.isAuthenticated());
        assertEquals(3, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "READONLY".equals(au.getAuthority()));
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_ADMIN".equals(au.getAuthority()));
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_WRITE".equals(au.getAuthority()));
    }

    @Test
    public void old_token_gives_write_permission() {
        JWT.decode(auth.substring(6).trim());

        final DecodedJWT mockDecodedJWT = Mockito.mock(DecodedJWT.class);
        Claim claim = Mockito.mock(Claim.class);
        when(claim.asString())
                .thenReturn(
                        "taxonomy-prod:write drafts-staging:write articles-test:publish images-prod:write listing-test:write articles-brukertest:write listing-prod:write articles-test:write articles-staging:write listing-brukertest:write images-staging:write images-test:write articles-prod:publish articles-brukertest:publish drafts-test:set_to_publish listing-staging:write audio-brukertest:write drafts-prod:write images-brukertest:write drafts-test:write audio-prod:write audio-test:write articles-staging:publish articles-prod:write drafts-staging:set_to_publish audio-staging:write drafts-brukertest:set_to_publish drafts-prod:set_to_publish drafts-brukertest:write drafts-taxonomy:write ");
        when(mockDecodedJWT.getClaim("scope")).thenReturn(claim);
        JWTAuthentication token = new JWTAuthentication(mockDecodedJWT);
        assertEquals(2, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_WRITE".equals(au.getAuthority()));
    }

    @Test
    public void no_scope_gives_readonly() {
        final DecodedJWT mockDecodedJWT = Mockito.mock(DecodedJWT.class);
        Claim claim = Mockito.mock(Claim.class);
        when(claim.asString()).thenReturn(null);
        when(mockDecodedJWT.getClaim("scope")).thenReturn(claim);
        JWTAuthentication token = new JWTAuthentication(mockDecodedJWT);
        assertEquals(1, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "READONLY".equals(au.getAuthority()));
    }
}
