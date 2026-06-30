/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import no.ndla.common.auth.Permission
import no.ndla.common.auth.Permission.*
import no.ndla.common.configuration.BaseProps

import java.util.Date
import scala.jdk.CollectionConverters.*

object NdlaAuthTestTokens {
  lazy val NoPermissions: String  = mkJWT()
  lazy val AllPermissions: String = mkJWT(Permission.values*)

  lazy val ArticleWrite: String = mkJWT(ARTICLE_API_WRITE)

  lazy val AudioWrite: String = mkJWT(AUDIO_API_WRITE)

  lazy val ConceptWrite: String = mkJWT(CONCEPT_API_WRITE)

  lazy val DraftWrite: String = mkJWT(DRAFT_API_WRITE)

  lazy val FrontPageAdmin: String = mkJWT(FRONTPAGE_API_ADMIN)

  lazy val ImageWrite: String = mkJWT(IMAGE_API_WRITE)
  lazy val ImageBatch: String = mkJWT(IMAGE_API_BATCH)

  lazy val LearningPathAdmin: String         = mkJWT(LEARNINGPATH_API_ADMIN)
  lazy val LearningPathWrite: String         = mkJWT(LEARNINGPATH_API_WRITE)
  lazy val LearningPathAdminAndWrite: String = mkJWT(LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_WRITE)

  private val rsaJwk                    = TestRsaJwk.NdlaAuthKey
  private lazy val signer: RSASSASigner = new RSASSASigner(rsaJwk)

  private def mkJWT(permissions: Permission*): String = {
    val props = new BaseProps {
      override def ApplicationName: String          = ""
      override def ApplicationPort: Int             = 0
      override def ndlaAuth0Scopes: Seq[Permission] = Seq.empty
    }
    val iss = props.ndlaAuth0Issuer
    val aud = props.ndlaAuth0Audience

    val claims = new JWTClaimsSet.Builder()
      .issueTime(new Date())
      .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
      .issuer(iss)
      .audience(aud)
      .subject("google-oauth2|SomeGoogleNumber")
      .claim("azp", "SomeClientId")
      .claim("scope", "openid profile email offline_access")
      .claim("https://ndla.no/client_id", "SomeClientId")
      .claim("https://ndla.no/ndla_id", "SomeNdlaId")
      .claim("https://ndla.no/user_email", "cool.user@example.com")
      .claim("https://ndla.no/user_name", "Some cool user")
      .claim("permissions", permissions.map(_.entryName).asJava)
      .build()
    val header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID).`type`(JOSEObjectType.JWT).build()
    val jwt    = new SignedJWT(header, claims)
    jwt.sign(signer)
    jwt.serialize()
  }
}
