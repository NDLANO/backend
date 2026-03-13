/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.{OneOf, OneOfVariant}
import sttp.tapir.*
import sttp.tapir.generic.auto.*

object TapirUtil extends StrictLogging {
  private def variantsForCodes(codes: Seq[Int]): Seq[OneOfVariant[AllErrors]] = codes.map(errorOutputVariantFor)

  def errorOutputVariantFor(code: Int): OneOfVariant[AllErrors] = {
    val statusCode = StatusCode(code)
    oneOfVariantValueMatcher(statusCode, NoNullJsonPrinter.jsonBody[AllErrors]) { case errorBody: AllErrors =>
      errorBody.statusCode == statusCode.code
    }
  }

  private def undocumentedStatusDefaultVariant(documentedCodes: Set[Int]): OneOfVariant[AllErrors] =
    oneOfDefaultVariant(
      statusCode
        .and(NoNullJsonPrinter.jsonBody[AllErrors])
        .map { case (_, err) =>
          err
        } { err =>
          if (!documentedCodes.contains(err.statusCode)) {
            logger.error(s"""Returned status ${err.statusCode}, but it is not documented for this endpoint.
                            |Documented statuses are: ${documentedCodes.toList.sorted.mkString(", ")}.
                            |OpenAPI will not list this status code.""".stripMargin)
          }
          (StatusCode(err.statusCode), err)
        }
    )

  def errorOutputsFor(codes: Int*): OneOf[AllErrors, AllErrors] = {
    val non500DefaultCodes   = List(400, 404)
    val codesToGetVariantFor = (
      codes ++ non500DefaultCodes
    ).distinct
    val variants       = variantsForCodes(codesToGetVariantFor)
    val defaultVariant = undocumentedStatusDefaultVariant(codesToGetVariantFor.toSet)
    val err            = variants :+ defaultVariant

    oneOf[AllErrors](err.head, err.tail*)
  }
}
