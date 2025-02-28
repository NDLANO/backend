/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration.nodebb

import cats.implicits.catsSyntaxOptionId
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto.*
import io.circe.parser.parse
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.myndlaapi.Props
import no.ndla.network.model.FeideAccessToken
import sttp.client3.Response
import sttp.client3.quick.*
import sttp.model.headers.CookieWithMeta

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait NodeBBClient {
  this: Props =>
  val nodebb: NodeBBClient

  class NodeBBClient extends StrictLogging {
    private val baseUrl: String = props.nodeBBUrl
    private val attemptLimit    = 5

    case class NodeBBSession(csrfToken: String, cookies: Seq[CookieWithMeta])

    private def getCSRFToken(feideToken: FeideAccessToken): Try[NodeBBSession] = {
      val request = quickRequest
        .get(uri"$baseUrl/api/config")
        .header("FeideAuthorization", s"Bearer $feideToken")
      val resp = doReq(request).?
      val csrfToken = parse(resp.body)
        .flatMap(_.as[NodeBBApiConfig])
        .toTry
        .map(_.csrf_token)
        .?

      Success(NodeBBSession(csrfToken, resp.unsafeCookies))
    }

    @tailrec
    private def doReq(request: sttp.client3.Request[String, Any], attempt: Int = 1): Try[Response[String]] = {
      Try {
        simpleHttpClient.send(request)
      } match {
        case Failure(ex) =>
          // NOTE: For some reason nodebb sometimes replies with GOAWAY if we do a few requests
          //       Not really sure why, but this works around that without too much hassle :^)
          if (attempt > attemptLimit) Failure(ex)
          else {
            logger.warn(
              s"Failed to do request, attempt $attempt: ${ex.getMessage}, ${Option(ex.getCause).map(_.getMessage)}"
            )
            doReq(request, attempt + 1)
          }
        case Success(value) => Success(value)
      }
    }

    def getUserId(feideToken: FeideAccessToken): Try[Option[Long]] = {
      val request = quickRequest
        .get(uri"$baseUrl/api/config")
        .header("FeideAuthorization", s"Bearer $feideToken")
      val resp = doReq(request).?

      if (resp.code.code == 403) return Success(None)

      val body = resp.body
      parse(body)
        .flatMap(_.as[UserSelf])
        .toTry
        .map(_.uid.some)
    }

    def deleteUser(userId: Option[Long], feideToken: FeideAccessToken): Try[Unit] = {
      userId match {
        case None => Success(())
        case Some(id) =>
          for {
            nodebbSession <- getCSRFToken(feideToken)
            _             <- deleteUserWithCSRF(id, feideToken, nodebbSession)
          } yield ()
      }
    }

    def deleteUserWithCSRF(userId: Long, feideToken: FeideAccessToken, nodebbSession: NodeBBSession): Try[Unit] = {
      val request = quickRequest
        .delete(uri"$baseUrl/api/v3/users/$userId/account")
        .header("FeideAuthorization", s"Bearer $feideToken")
        .header("X-CSRF-Token", nodebbSession.csrfToken)
        .cookies(nodebbSession.cookies)
      val resp = doReq(request).?
      if (resp.isSuccess) Success(())
      else {
        val msg = s"Failed to delete nodebb user with id $userId, Got code: ${resp.code}, with body:\n\n${resp.body}"
        logger.error(msg)
        Failure(new Exception(msg))
      }
    }
  }
}
