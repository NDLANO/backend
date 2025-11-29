/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.Environment.{booleanPropOrFalse, unsafeProp}
import no.ndla.common.model.api.search.SearchType
import no.ndla.common.model.domain.Content
import no.ndla.search.model.domain.ReindexResult
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.service.search.IndexService
import no.ndla.searchapi.{ComponentRegistry, SearchApiProperties}
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import scala.util.Properties.propOrElse
import java.time.Instant

/** This part of search-api is used for indexing in a separate instance. If enabled, this will also send a slack message
  * if the indexing fails for any reason.
  */
class StandaloneIndexing(props: SearchApiProperties, componentRegistry: ComponentRegistry) extends StrictLogging {
  case class SlackAttachment(title: String, color: String, ts: String, text: String)
  object SlackAttachment {
    implicit val encoder: Encoder[SlackAttachment] = deriveEncoder
    implicit val decoder: Decoder[SlackAttachment] = deriveDecoder
  }

  case class SlackPayload(channel: String, username: String, attachments: Seq[SlackAttachment])

  object SlackPayload {
    implicit val encoder: Encoder[SlackPayload] = deriveEncoder
    implicit val decoder: Decoder[SlackPayload] = deriveDecoder
  }

  def sendSlackError(errors: Seq[String]): Unit = {
    val enableSlackMessageFlag = "SLACK_ERROR_ENABLED"
    if (!booleanPropOrFalse(enableSlackMessageFlag)) {
      logger.info(s"Skipping sending message to slack because $enableSlackMessageFlag...")
      return
    } else {
      logger.info("Sending message to slack...")
    }

    val errorTitle = s"search-api ${props.Environment}"
    val errorBody  = s"Standalone indexing failed with:\n${errors.mkString("\n")}"

    val errorAttachment =
      SlackAttachment(color = "#ff0000", ts = Instant.now.getEpochSecond.toString, title = errorTitle, text = errorBody)

    val payload = SlackPayload(
      channel = propOrElse("SLACK_CHANNEL", "ndla-indexing-errors"),
      username = propOrElse("SLACK_USERNAME", "indexbot"),
      attachments = Seq(errorAttachment),
    )

    val body = CirceUtil.toJsonString(payload)

    val url = propOrElse("SLACK_URL", "https://slack.com/api/chat.postMessage")

    simpleHttpClient.send(
      quickRequest
        .post(uri"$url")
        .body(body)
        .header("Content-Type", "application/json", replaceExisting = true)
        .header("Authorization", s"Bearer ${unsafeProp(s"SLACK_TOKEN")}")
    ): Unit
  }

  def doStandaloneIndexing(): Nothing = {
    val bundles = for {
      grepBundle   <- componentRegistry.grepApiClient.getGrepBundle()
      myndlaBundle <- componentRegistry.myndlaApiClient.getMyNDLABundle
    } yield (grepBundle, myndlaBundle)

    val start = System.currentTimeMillis()

    val reindexResult = bundles match {
      case Failure(ex)                         => Seq(Failure(ex))
      case Success((grepBundle, myndlaBundle)) =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchType.values.size))

        def handleOnComplete(future: Future[Try[ReindexResult]], indexName: String): Future[Try[ReindexResult]] = {
          future.onComplete {
            case Success(Success(reindexResult: ReindexResult)) => logger.info(
                s"Completed indexing of ${reindexResult.totalIndexed} $indexName in ${reindexResult.millisUsed} ms."
              )
            case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
            case Failure(ex)          => logger.warn(s"Unable to create index '$indexName': " + ex.getMessage, ex)
          }
          future
        }

        val indexingBundle =
          IndexingBundle(grepBundle = Some(grepBundle), taxonomyBundle = None, myndlaBundle = Some(myndlaBundle))

        def reindexWithIndexService[C <: Content](
            indexService: IndexService[C]
        )(implicit d: Decoder[C]): Future[Try[ReindexResult]] = {
          val reindexFuture = Future(indexService.indexDocuments(indexingBundle))
          handleOnComplete(reindexFuture, indexService.searchIndex)
        }

        Await.result(
          Future.sequence(
            Seq(
              reindexWithIndexService(componentRegistry.learningPathIndexService),
              reindexWithIndexService(componentRegistry.articleIndexService),
              reindexWithIndexService(componentRegistry.draftIndexService),
              reindexWithIndexService(componentRegistry.draftConceptIndexService),
              handleOnComplete(
                Future(componentRegistry.grepIndexService.indexDocuments(None, indexingBundle.grepBundle)),
                "greps",
              ),
              handleOnComplete(Future(componentRegistry.nodeIndexService.indexDocuments(None, indexingBundle)), "nodes"),
            )
          ),
          Duration.Inf,
        )
    }

    val errors = reindexResult.collect {
      case Success(ReindexResult(name, numErrors, totalIndexed, _)) if numErrors > 0 =>
        val totalDocuments = numErrors + totalIndexed
        s"Indexing of '$name' finished indexing with $numErrors errors ($totalIndexed/$totalDocuments)"
      case Failure(ex) =>
        logger.error("Indexing failed...", ex)
        ex.getMessage
    }

    if (errors.nonEmpty) {
      sendSlackError(errors)
      sys.exit(1)
    }

    logger.info(s"Reindexing all indexes took ${System.currentTimeMillis() - start} ms...")
    sys.exit(0)
  }
}
