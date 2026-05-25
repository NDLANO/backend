/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.network.tapir.{AllErrors, ErrorHandling, ErrorHelpers}

/** Combines each per-app [[ErrorHandling]] so domain-specific exceptions from any app are matched by their owning app's
  * handler. Falls through to [[ErrorHandling]]'s default unknown-error handling if no per-app PartialFunction matches.
  */
class CompositeErrorHandling(handlers: Seq[ErrorHandling])(using errorHelpers: ErrorHelpers) extends ErrorHandling {
  override def handleErrors: PartialFunction[Throwable, AllErrors] = handlers
    .map(_.handleErrors)
    .reduceLeftOption(_ orElse _)
    .getOrElse(PartialFunction.empty)
}
