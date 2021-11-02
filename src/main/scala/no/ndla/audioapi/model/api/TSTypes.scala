package no.ndla.audioapi.model.api

import com.scalatsi._
import com.scalatsi.dsl._

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
  implicit val author: TSIType[Author] = TSType.fromCaseClass[Author]
  // implicit val validationMessage: TSIType[ValidationMessage] = TSType.fromCaseClass[ValidationMessage]

  implicit val SeriesSummaryTSI: TSIType[SeriesSummary] = {
    implicit val audioSummaryReference: TSType[AudioSummary] = TSType.external[AudioSummary]("IAudioSummary")
    TSType.fromCaseClass[SeriesSummary]
  }

  implicit val audioMetaInformationTSI: TSIType[AudioMetaInformation] = {
    implicit val audioMetaInformationReference: TSType[Series] = TSType.external[Series]("ISeries")
    TSType.fromCaseClass[AudioMetaInformation]
  }

  implicit val seriesTSI: TSIType[Series] = {
    implicit val audioMetaInformationReference: TSType[AudioMetaInformation] =
      TSType.external[AudioMetaInformation]("IAudioMetaInformation")
    TSType.fromCaseClass[Series]
  }

  implicit val AudioSummaryTSI: TSIType[AudioSummary] = {
    implicit val seriesSummaryReference: TSType[SeriesSummary] = TSType.external[SeriesSummary]("ISeriesSummary")
    TSType.fromCaseClass[AudioSummary]
  }
}
