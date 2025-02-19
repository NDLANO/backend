/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.logging.logTaskTime
import no.ndla.common.model.NDLADate
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.grep.*
import sttp.client3.quick.*

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration.DurationInt
import scala.util.Using.Releasable
import scala.util.{Failure, Success, Try, Using}

trait GrepApiClient {
  this: NdlaClient & Props =>
  val grepApiClient: GrepApiClient

  class GrepApiClient extends StrictLogging {
    import props.GrepApiUrl
    private val grepDumpUrl = s"$GrepApiUrl/kl06/v201906/dump/json"

    private def readFile(file: File): Try[String] = Try {
      Using.resource(scala.io.Source.fromFile(file)) { source =>
        source.getLines().mkString
      }
    }

    private def readGrepJsonFiles[T](dump: File, path: String)(implicit d: Decoder[T]): Try[List[T]] = {
      val folder    = new File(dump, path)
      val jsonFiles = folder.list()
      jsonFiles.toList.traverse { f =>
        for {
          jsonStr <- readFile(new File(folder, f))
          parsed  <- CirceUtil.tryParseAs[T](jsonStr)
        } yield parsed
      }
    }

    private def getKjerneelementerLK20(dump: File): Try[List[GrepKjerneelement]] =
      readGrepJsonFiles[GrepKjerneelement](dump, "kjerneelementer-lk20")
    private def getKompetansemaalLK20(dump: File): Try[List[GrepKompetansemaal]] =
      readGrepJsonFiles[GrepKompetansemaal](dump, "kompetansemaal-lk20")
    private def getKompetansemaalsettLK20(dump: File): Try[List[GrepKompetansemaalSett]] =
      readGrepJsonFiles[GrepKompetansemaalSett](dump, "kompetansemaalsett-lk20")
    private def getTverrfagligeTemaerLK20(dump: File): Try[List[GrepTverrfagligTema]] =
      readGrepJsonFiles[GrepTverrfagligTema](dump, "tverrfaglige-temaer-lk20")
    private def getLaereplanerLK20(dump: File): Try[List[GrepLaererplan]] =
      readGrepJsonFiles[GrepLaererplan](dump, "laereplaner-lk20")

    private def getBundleFromDump(dump: File): Try[GrepBundle] = for {
      kjerneelementer    <- getKjerneelementerLK20(dump)
      kompetansemaal     <- getKompetansemaalLK20(dump)
      kompetansemaalsett <- getKompetansemaalsettLK20(dump)
      tverrfagligeTemaer <- getTverrfagligeTemaerLK20(dump)
      laereplaner        <- getLaereplanerLK20(dump)
    } yield GrepBundle(
      kjerneelementer = kjerneelementer,
      kompetansemaal = kompetansemaal,
      kompetansemaalsett = kompetansemaalsett,
      tverrfagligeTemaer = tverrfagligeTemaer,
      laereplaner = laereplaner
    )

    val getGrepBundle: () => Try[GrepBundle]                   = () => _getGrepBundle(())
    private val _getGrepBundle: Memoize[Unit, Try[GrepBundle]] = new Memoize(1000 * 60, _ => getGrepBundleUncached)

    implicit object FileIsReleasable extends Releasable[File] {
      private def deleteDirectory(f: File): Unit = {
        if (f.isDirectory) {
          f.listFiles().foreach(deleteDirectory)
        }
        f.delete(): Unit
      }
      def release(resource: File): Unit = deleteDirectory(resource)
    }

    private def getGrepBundleUncached: Try[GrepBundle] = logTaskTime("Fetching grep bundle", 30.seconds) {
      val date        = NDLADate.now().toUTCEpochSecond
      val tempDirPath = Try(Files.createTempDirectory(s"grep-dump-$date")).?
      Using(tempDirPath.toFile) { tempDir =>
        val zippedDump   = fetchDump(tempDir).?
        val unzippedDump = ZipUtil.unzip(zippedDump, tempDir, deleteArchive = true).?
        getBundleFromDump(unzippedDump).?
      }
    }

    case class GrepDumpDownloadException(message: String) extends RuntimeException(message) {
      def withCause(cause: Throwable): GrepDumpDownloadException = {
        initCause(cause)
        this
      }
    }

    private def fetchDump(tempDir: File): Try[File] = {
      val outputFile = new File(tempDir, "grep-dump.zip")
      logger.info(s"Downloading grep dump from $grepDumpUrl to ${outputFile.getAbsolutePath}")
      val request = quickRequest
        .get(uri"$grepDumpUrl")
        .response(asFile(outputFile))
      Try(simpleHttpClient.send(request)) match {
        case Success(response) if response.isSuccess => Success(outputFile)
        case Success(response) =>
          Failure(GrepDumpDownloadException(s"Failed to fetch grep dump: ${response.statusText}"))
        case Failure(ex) =>
          Failure(GrepDumpDownloadException(s"Failed to fetch grep dump: ${ex.getMessage}").withCause(ex))
      }
    }

  }
}
