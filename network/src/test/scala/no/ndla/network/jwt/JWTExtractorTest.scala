/*
 * Part of NDLA network
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.jwt

import no.ndla.network.UnitSuite
import no.ndla.network.model.NdlaHttpRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

class JWTExtractorTest extends UnitSuite {

  val token =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwwMDAwMDAwMDAwIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJhenAiOiJXVTBLcjRDRGtyTTB1TCIsImV4cCI6MTUwNjM1Mjg2MSwiaWF0IjoxNTA2MzQ2OTAzLCJzY29wZSI6Imxpc3Rpbmc6d3JpdGUgZHJhZnRzOndyaXRlIiwiaHR0cHM6Ly9uZGxhLm5vL25kbGFfaWQiOiJkZXR0ZV9lcl9lbl9uZGxhX2lkIiwiaHR0cHM6Ly9uZGxhLm5vL3VzZXJfbmFtZSI6IlRlc3QgVGVzdGVzZW4iLCJqdGkiOiI4OTMwMDY4YS0zMTkzLTRjYjgtOGI1Ni1lNzU3MjdhYjdmZDUifQ.X-3bTtv8f_DUEgy60wkDazO12gtoeEbXKRHQV4Sd8rQ"

  val tokenWithCustomClientIdField =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwwMDAwMDAwMDAwIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJhenAiOiJXVTBLcjRDRGtyTTB1TCIsImV4cCI6MTUwNjM1Mjg2MSwiaWF0IjoxNTA2MzQ2OTAzLCJzY29wZSI6Imxpc3RpbmctdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOndyaXRlIGF1ZGlvLXN0YWdpbmc6d3JpdGUgZHJhZnRzOndyaXRlIiwiaHR0cHM6Ly9uZGxhLm5vL25kbGFfaWQiOiJkZXR0ZV9lcl9lbl9uZGxhX2lkIiwiaHR0cHM6Ly9uZGxhLm5vL3VzZXJfbmFtZSI6IlRlc3QgVGVzdGVzZW4iLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoia2xkc2ZqYWxza2RmaiIsImp0aSI6Ijg5MzAwNjhhLTMxOTMtNGNiOC04YjU2LWU3NTcyN2FiN2ZkNSJ9.lXCVD0dGN564GZa6MYwcUA40eD_8IO1kVEfXdLgTB3g"

  test("That userId is None when no authorization header is set") {
    val request = mock[NdlaHttpRequest]
    when(request.getHeader(any[String])).thenReturn(None)
    JWTExtractor(request).extractUserId() should be(None)
  }

  test("That userId is None when an illegal JWT is set") {
    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some("This is an invalid JWT"))

    JWTExtractor(request).extractUserId() should be(None)
  }

  test("That userId is None when no ndla_id is present") {
    val tokenWithoutUserId =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MDAwMDAwMDAwMCIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiV1UwS3I0Q0Rrck0wdUwiLCJleHAiOjE1MDY1MTg3NjQsImlhdCI6MTUwNjM0NjkwMywic2NvcGUiOiJsaXN0aW5nLXRlc3Q6d3JpdGUgYXJ0aWNsZXMtdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOndyaXRlIGF1ZGlvLXN0YWdpbmc6d3JpdGUiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiVGVzdCBUZXN0ZXNlbiIsImp0aSI6IjNmYmNlNDk1LTlmMDMtNDE0Ny1hNjcyLTVmZjYzOGVjMDkyNCJ9.aWQ2lqSXzsMgr_dd5S5xHKKGqPVRX7LdnH2nkSUlM-0"
    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $tokenWithoutUserId"))
    JWTExtractor(request).extractUserId() should be(None)
  }

  test("That JWTExtractor.extractUserId is set even if roles are not present") {
    val tokenWithoutRoles =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MDAwMDAwMDAwMCIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiV1UwS3I0Q0Rrck0wdUwiLCJleHAiOjE1MDY1MTc1ODQsImlhdCI6MTUwNjM0NjkwMywiaHR0cHM6Ly9uZGxhLm5vL25kbGFfaWQiOiJkZXR0ZV9lcl9lbl9uZGxhX2lkIiwiaHR0cHM6Ly9uZGxhLm5vL3VzZXJfbmFtZSI6IlRlc3QgVGVzdGVzZW4iLCJqdGkiOiJiNGVlZmQwZi0zNjg1LTQwMWItYjY3MC02MzUyY2NmNGQzNTgifQ.8vEYDokCZYAIz1Vq7R-NfU0NrcI9hpoIL7316fFYF_A"
    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $tokenWithoutRoles"))
    val jWTExtractor = JWTExtractor(request)
    jWTExtractor.extractUserId() should equal(Some("dette_er_en_ndla_id"))
    jWTExtractor.extractUserRoles() should equal(List.empty)
  }

  test("That all roles for correct environment are extracted") {
    withEnv("NDLA_ENVIRONMENT", Some("test")) {
      val request = mock[NdlaHttpRequest]
      when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $token"))

      val jwtExtractor = JWTExtractor(request)
      val roles        = jwtExtractor.extractUserRoles()
      roles.size should be(2)
      roles.contains("listing:write") should be(true)
      roles.contains("drafts:write") should be(true)
    }
  }

  test("That name is extracted") {
    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $token"))

    val jwtExtractor = JWTExtractor(request)
    jwtExtractor.extractUserName() should equal(Some("Test Testesen"))
  }

  test("client id should be extracted from the 'azp' field instead of custom ndla client_id field") {
    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $tokenWithCustomClientIdField"))

    val jwtExtractor = JWTExtractor(request)
    jwtExtractor.extractClientId() should equal(Some("WU0Kr4CDkrM0uL"))
  }

  test("Permissions in tokens should work as expected and be merged with scope") {
    val token =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZnNleE9DZkpGR09LdXkxQzJlNzFPc3ZRd3EwTldLQUtAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNjM0NzIzNTM5LCJleHAiOjE2MzQ3MjcxMzksImF6cCI6ImZzZXhPQ2ZKRkdPS3V5MUMyZTcxT3N2UXdxME5XS0FLIiwic2NvcGUiOiJhcnRpY2xlczpwdWJsaXNoIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIiwicGVybWlzc2lvbnMiOlsiYXJ0aWNsZXM6d3JpdGUiLCJhdWRpbzp3cml0ZSIsImNvbmNlcHQ6d3JpdGUiLCJkcmFmdHM6YWRtaW4iLCJkcmFmdHM6cHVibGlzaCIsImRyYWZ0czp3cml0ZSIsImltYWdlczp3cml0ZSIsImxlYXJuaW5ncGF0aDphZG1pbiIsImxlYXJuaW5ncGF0aDpwdWJsaXNoIiwibGVhcm5pbmdwYXRoOndyaXRlIiwidGF4b25vbXk6YWRtaW4iLCJ0YXhvbm9teTp3cml0ZSJdfQ.3iGSXuBaFtRmKvWYHu2C6zi2WhQ3yQhEmoVelp7pGCg"

    val request = mock[NdlaHttpRequest]
    when(request.getHeader("Authorization")).thenReturn(Some(s"Bearer $token"))

    val jw  = JWTExtractor(request)
    val res = jw.extractUserRoles()
    res should be(
      List(
        "articles:publish",
        "articles:write",
        "audio:write",
        "concept:write",
        "drafts:admin",
        "drafts:publish",
        "drafts:write",
        "images:write",
        "learningpath:admin",
        "learningpath:publish",
        "learningpath:write",
        "taxonomy:admin",
        "taxonomy:write",
      )
    )
  }
}
