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
  private def variantsForCodes(codes: Seq[Int]): Seq[OneOfVariant[AllErrors]] = codes
    .map(code => {
      val statusCode = StatusCode(code)
      oneOfVariantValueMatcher(statusCode, NoNullJsonPrinter.jsonBody[AllErrors]) { case errorBody: AllErrors =>
        errorBody.statusCode == statusCode.code
      }
    })

  private val internalServerErrorDefaultVariant: OneOfVariant[ErrorBody] = oneOfDefaultVariant(
    statusCode(StatusCode.InternalServerError)
      .and(NoNullJsonPrinter.jsonBody[ErrorBody])
      .map(err => err)(err => {
        if (err.statusCode != 500) {
          logger.error(s"Returned 500 even if the StatusCode did not match. This seems like a bug. The error was: $err")
        }
        err
      })
  )

  def errorOutputsFor(codes: Int*): OneOf[AllErrors, AllErrors] = {
    val non500DefaultCodes   = List(400, 404)
    val codesToGetVariantFor = (codes ++ non500DefaultCodes).distinct
    val variants             = variantsForCodes(codesToGetVariantFor)
    val err                  = variants :+ internalServerErrorDefaultVariant

    oneOf[AllErrors](err.head, err.tail*)
  }
}
