/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */
package no.ndla.searchapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.{booleanPropOrFalse, prop}
import no.ndla.common.model.domain.Content
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.{ComponentRegistry, SearchApiProperties}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import sttp.client3.quick._

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
  case class SlackAttachment(
      title: String,
      color: String,
      ts: String,
      text: String
  )

  case class SlackPayload(
      channel: String,
      username: String,
      attachments: Seq[SlackAttachment]
  )

  def sendSlackError(errors: Seq[String]): Unit = {
    val enableSlackMessageFlag = "SLACK_ERROR_ENABLED"
    if (!booleanPropOrFalse(enableSlackMessageFlag)) {
      logger.info(s"Skipping sending message to slack because $enableSlackMessageFlag...")
      return
    } else {
      logger.info("Sending message to slack...")
    }

    implicit val formats: Formats = DefaultFormats
    val errorTitle                = s"search-api ${props.Environment}"
    val errorBody                 = s"Standalone indexing failed with:\n${errors.mkString("\n")}"

    val errorAttachment = SlackAttachment(
      color = "#ff0000",
      ts = Instant.now.getEpochSecond.toString,
      title = errorTitle,
      text = errorBody
    )

    val payload = SlackPayload(
      channel = propOrElse("SLACK_CHANNEL", "ndla-indexing-errors"),
      username = propOrElse("SLACK_USERNAME", "indexbot"),
      attachments = Seq(errorAttachment)
    )

    val body = Serialization.write(payload)

    val url = propOrElse("SLACK_URL", "https://slack.com/api/chat.postMessage")

    simpleHttpClient.send(
      quickRequest
        .post(uri"$url")
        .body(body)
        .header("Content-Type", "application/json", replaceExisting = true)
        .header("Authorization", s"Bearer ${prop(s"SLACK_TOKEN")}")
    )
  }

  def doStandaloneIndexing(): Nothing = {
    val bundles = for {
      taxonomyBundle <- componentRegistry.taxonomyApiClient.getTaxonomyBundle()
      grepBundle     <- componentRegistry.grepApiClient.getGrepBundle()
    } yield (taxonomyBundle, grepBundle)

    val start = System.currentTimeMillis()

    val reindexResult = bundles match {
      case Failure(ex) => Seq(Failure(ex))
      case Success((taxonomyBundle, grepBundle)) =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(props.SearchIndexes.size))

        def reindexWithIndexService[C <: Content](
            indexService: componentRegistry.IndexService[C]
        )(implicit mf: Manifest[C]): Future[Try[ReindexResult]] = {
          val reindexFuture = Future {
            indexService.indexDocuments(taxonomyBundle, grepBundle)
          }
          reindexFuture.onComplete {
            case Success(Success(reindexResult: ReindexResult)) =>
              logger.info(
                s"Completed indexing of ${reindexResult.totalIndexed} ${indexService.searchIndex} in ${reindexResult.millisUsed} ms."
              )
            case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
            case Failure(ex) =>
              logger.warn(s"Unable to create index '${indexService.searchIndex}': " + ex.getMessage, ex)
          }

          reindexFuture
        }

        Await.result(
          Future.sequence(
            Seq(
              reindexWithIndexService(componentRegistry.learningPathIndexService),
              reindexWithIndexService(componentRegistry.articleIndexService),
              reindexWithIndexService(componentRegistry.draftIndexService)
            )
          ),
          Duration.Inf
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
