/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import no.ndla.common.auth.Permission
import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.{MyNDLAUser, UserRole}
import no.ndla.network.model.{FeideIdToken, FeideUserWrapper}

import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.*

object FeideAuthTestData {
  lazy val FrankForeleser: FeideUserWrapper = mkFeide(
    1L,
    UserRole.EMPLOYEE,
    email = "noreply@feide.no",
    name = "Frank Foreleser Føllesen",
    principalName = "frank_foreleser@testusers.feide.no",
  )
  lazy val AnneLaerer: FeideUserWrapper = mkFeide(
    2L,
    UserRole.EMPLOYEE,
    email = "noreply@feide.no",
    name = "Anne LærerVGS Haugen",
    principalName = "anne_laerervgs@testusers.feide.no",
  )
  lazy val AsbjornElev: FeideUserWrapper = mkFeide(
    3L,
    UserRole.STUDENT,
    email = "noreply@feide.no",
    name = "Asbjørn ElevG Hansen",
    principalName = "asbjorn_elevg@testusers.feide.no",
  )

  private val props = new BaseProps {
    override def ApplicationName: String          = ""
    override def ApplicationPort: Int             = 0
    override def ndlaAuth0Scopes: Seq[Permission] = Seq.empty
  }
  private val iss = props.feideIssuer
  private val aud = props.feideClientId.getOrElse("feide-client-id")

  private def mkFeide(
      id: Long,
      role: UserRole,
      email: String,
      name: String,
      principalName: String,
  ): FeideUserWrapper = {
    val idToken = FeideIdToken(
      iss = iss,
      jti = UUID.randomUUID().toString,
      aud = List(aud),
      sub = UUID.randomUUID().toString,
      iat = 0L,
      exp = 0L,
      email = email,
      name = name,
      userid_sec = List(s"feide:$principalName"),
      eduPersonPrincipalName = principalName,
      originalToken = "",
    )
    val jwt  = mkJWT(idToken)
    val user = MyNDLAUser(
      id = id,
      feideId = idToken.sub,
      username = idToken.eduPersonPrincipalName,
      email = idToken.email,
      displayName = idToken.name,
      favoriteSubjects = Seq.empty,
      userRole = role,
      organization = "",
      groups = Seq.empty,
      arenaEnabled = false,
      lastUpdated = NDLADate.now(),
      lastSeen = NDLADate.now(),
    )
    FeideUserWrapper(user, idToken.copy(originalToken = jwt), UUID.randomUUID().toString)
  }

  private val rsaJwk                    = TestRsaJwk.FeideAuthKey
  private lazy val signer: RSASSASigner = new RSASSASigner(rsaJwk)

  private def mkJWT(idToken: FeideIdToken): String = {
    val now    = new Date()
    val claims = new JWTClaimsSet.Builder()
      .issueTime(now)
      .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
      .issuer(idToken.iss)
      .audience(idToken.aud.asJava)
      .subject(idToken.sub)
      .jwtID(idToken.jti)
      .claim("email", idToken.email)
      .claim("name", idToken.name)
      .claim("https://n.feide.no/claims/userid_sec", idToken.userid_sec.asJava)
      .claim("https://n.feide.no/claims/eduPersonPrincipalName", idToken.eduPersonPrincipalName)
      .build()
    val header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID).`type`(JOSEObjectType.JWT).build()
    val jwt    = new SignedJWT(header, claims)
    jwt.sign(signer)
    jwt.serialize()
  }
}
