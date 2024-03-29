/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.integration.nodebb

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import io.circe.parser.parse
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.myndlaapi.Props
import sttp.client3.Response
import sttp.client3.quick._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait NodeBBClient {
  this: Props =>
  val nodebb: NodeBBClient

  class NodeBBClient extends StrictLogging {
    private val baseUrl: String = props.nodeBBUrl
    private val attemptLimit    = 5
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

    def getCategories: Try[Categories] = Try {
      val request = quickRequest.get(uri"$baseUrl/api/categories")
      val resp    = doReq(request).?
      parse(resp.body).flatMap(_.as[Categories]).toTry
    }.flatten

    def getSingleCategory(cid: Long): Try[SingleCategory] = Try {
      val request = quickRequest.get(uri"$baseUrl/api/category/$cid")
      val resp    = doReq(request).?
      parse(resp.body).flatMap(_.as[SingleCategory]).toTry
    }.flatten

    def getSingleTopic(tid: Long, pageNum: Long): Try[SingleTopic] = Try {
      val request = quickRequest.get(uri"$baseUrl/api/topic/$tid?page=$pageNum")
      val resp    = doReq(request).?
      parse(resp.body).flatMap(_.as[SingleTopic]).toTry
    }.flatten

    def getSinglePost(pid: Long): Try[SinglePost] = Try {
      val request = quickRequest.get(uri"$baseUrl/api/v3/posts/$pid")
      val resp    = doReq(request).?
      parse(resp.body)
        .flatMap(_.as[SinglePostResponse])
        .toTry
        .map(_.response)
    }.flatten
  }
}
