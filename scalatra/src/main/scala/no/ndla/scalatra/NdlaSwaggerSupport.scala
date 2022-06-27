/*
 * Part of NDLA scalatra.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatra

import com.typesafe.scalalogging.LazyLogging
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger.SwaggerSupportSyntax.{ParameterBuilder, SwaggerParameterBuilder}
import org.scalatra.swagger.{ParamType, Parameter, SwaggerSupport}
import org.scalatra.util.NotNothing

trait NdlaSwaggerSupport extends NdlaControllerBase with LazyLogging with SwaggerSupport {

  case class Param[T](paramName: String, description: String)

  protected def asQueryParam[T: Manifest: NotNothing](param: Param[T]): ParameterBuilder[T] =
    queryParam[T](param.paramName).description(param.description)

  protected def asHeaderParam[T: Manifest: NotNothing](param: Param[T]): ParameterBuilder[T] =
    headerParam[T](param.paramName).description(param.description)

  protected def asPathParam[T: Manifest: NotNothing](param: Param[T]): ParameterBuilder[T] =
    pathParam[T](param.paramName).description(param.description)

  protected def asObjectFormParam[T: Manifest: NotNothing](param: Param[T]): SwaggerParameterBuilder = {
    val className = manifest[T].runtimeClass.getSimpleName
    val modelOpt  = models.get(className)

    modelOpt match {
      case Some(value) =>
        formParam(param.paramName, value).description(param.description)
      case None =>
        logger.error(s"${param.paramName} could not be resolved as object formParam, doing regular formParam.")
        formParam[T](param.paramName).description(param.description)
    }
  }

  protected def asFileParam(param: Param[_]): Parameter =
    Parameter(
      name = param.paramName,
      `type` = ValueDataType("file"),
      description = Some(param.description),
      paramType = ParamType.Form
    )

}
