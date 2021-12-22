/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */
package no.ndla.searchapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties.{booleanOrFalse, prop, propOrElse}
import no.ndla.searchapi.model.domain.{Content, ReindexResult}
import no.ndla.searchapi.{ComponentRegistry, SearchApiProperties}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import scalaj.http.Http

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import java.time.Instant

/**
  * This part of search-api is used for indexing in a separate instance.
  * If enabled, this will also send a slack message if the indexing fails for any reason.
  * */
object StandaloneIndexing extends LazyLogging {
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
    if (!booleanOrFalse(enableSlackMessageFlag)) {
      logger.info(s"Skipping sending message to slack because $enableSlackMessageFlag...")
      return
    } else {
      logger.info("Sending message to slack...")
    }

    implicit val formats: Formats = DefaultFormats
    val errorTitle = s"search-api ${SearchApiProperties.Environment}"
    val errorBody = s"Standalone indexing failed with:\n${errors.mkString("\n")}"

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

    Http(url = propOrElse("SLACK_URL", "https://slack.com/api/chat.postMessage"))
      .postData(data = body)
      .header("Content-Type", "application/json")
      .header("Authorization", s"Bearer ${prop(s"SLACK_TOKEN")}")
      .asString
  }

  def doStandaloneIndexing(): Nothing = {
    val bundles = for {
      taxonomyBundle <- ComponentRegistry.taxonomyApiClient.getTaxonomyBundle()
      grepBundle <- ComponentRegistry.grepApiClient.getGrepBundle()
    } yield (taxonomyBundle, grepBundle)

    val start = System.currentTimeMillis()

    val reindexResult = bundles match {
      case Failure(ex) => Seq(Failure(ex))
      case Success((taxonomyBundle, grepBundle)) =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchApiProperties.SearchIndexes.size))

        def reindexWithIndexService[C <: Content](indexService: ComponentRegistry.IndexService[C])(
            implicit mf: Manifest[C]): Future[Try[ReindexResult]] = {
          val reindexFuture = Future {
            indexService.indexDocuments(taxonomyBundle, grepBundle)
          }
          reindexFuture.onComplete {
            case Success(Success(reindexResult: ReindexResult)) =>
              logger.info(
                s"Completed indexing of ${reindexResult.totalIndexed} ${indexService.searchIndex} in ${reindexResult.millisUsed} ms.")
            case Success(Failure(ex)) => logger.warn(ex.getMessage, ex)
            case Failure(ex) =>
              logger.warn(s"Unable to create index '${indexService.searchIndex}': " + ex.getMessage, ex)
          }

          reindexFuture
        }

        Await.result(
          Future.sequence(
            Seq(
              reindexWithIndexService(ComponentRegistry.learningPathIndexService),
              reindexWithIndexService(ComponentRegistry.articleIndexService),
              reindexWithIndexService(ComponentRegistry.draftIndexService)
            )),
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
