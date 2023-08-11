/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model

import scalikejdbc._

import java.util.UUID
import scala.util.{Failure, Success, Try}

package object domain {
  type FeideID          = String
  type FeideAccessToken = String

  // Binders used for UUID binding of scalikejdbc results
  implicit val uuidBinder = Binders.of[Try[UUID]] {
    case v: UUID => Success(v)
    case v       => Failure(NDLASQLException(s"Parsing UUID type from '${v.toString}' was not possible."))
  }(v => (ps, idx) => ps.setObject(idx, v))

  implicit val uuidParameterFactory: ParameterBinderFactory[UUID] = ParameterBinderFactory[UUID] { v => (stmt, idx) =>
    stmt.setObject(idx, v)
  }

  implicit val maybeUuidBinder: Binders[Option[UUID]] = Binders.of[Option[UUID]] {
    case v: UUID => Some(v)
    case _       => None
  }(v =>
    (ps, idx) => {
      v match {
        case Some(value) =>
          ps.setObject(idx, value)
        case None => ps.setObject(idx, null)
      }
    }
  )
}
