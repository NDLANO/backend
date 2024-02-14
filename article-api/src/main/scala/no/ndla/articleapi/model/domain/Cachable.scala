/*
 * Part of NDLA article-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.network.tapir.DynamicHeaders
import sttp.model.Header
import sttp.model.headers.CacheDirective

import scala.util.Try

/** Wrapper class for content that can have different cachability attributes based on the content Useful for Articles
  * that require login
  *
  * One would use the class by using `Cachable.yes(value)` (Or `Cachable.no(value)`) for values that can be cached and
  * then use `returnValue.Ok()` in the controller to get the scalatra type with headers.
  */
case class Cachable[T](
    value: T,
    canBeCached: Boolean
) {

  /** Return a tuple of [[T]] and [[DynamicHeaders]] type with the value as jsonbody and correct 'cache-control' header
    * applied.
    */
  def Ok(headers: List[Header] = List.empty): (T, DynamicHeaders) = {
    val cacheHeaders = if (canBeCached) List.empty else List(Header.cacheControl(CacheDirective.Private))
    value -> DynamicHeaders(headers ++ cacheHeaders)
  }

  /** Return a [[Cachable]] object with the function applied to value Example:
    * ```
    * val a = Cachable.yes("TestString")
    * val b = a.map(s => s.toLowerCase())
    *
    * // a.value = "TestString"
    * // b.value = "teststring"
    * ```
    */
  def map[U](f: T => U): Cachable[U] = {
    copy(value = f(value))
  }

  def flatMap[U](f: T => Cachable[U]): Cachable[U] = {
    f(value)
  }
}

object Cachable {

  def yes[T <: Try[U], U](value: T): Try[Cachable[U]] =
    value.map(v => Cachable.yes(v))

  def no[T <: Try[U], U](value: T): Try[Cachable[U]] =
    value.map(v => Cachable.no(v))

  def yes[T](value: T): Cachable[T] =
    new Cachable(
      value = value,
      canBeCached = true
    )

  def no[T](value: T): Cachable[T] =
    new Cachable(
      value = value,
      canBeCached = false
    )
}
