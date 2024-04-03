/*
 * Part of NDLA article-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model

import no.ndla.articleapi._
import no.ndla.articleapi.model.domain.Cachable

import scala.util.{Success, Try}

class CachableTest extends UnitSuite with TestEnvironment {
  test("That map works as expected") {
    val c1: Cachable[Int] = Cachable.no(1)
    c1.map(x => x + 5) should be(Cachable(6, false))
  }

  test("That for-comprehensions (flatMap) works as expected") {
    val c1: Cachable[Int] = Cachable.yes(1)
    val c2: Cachable[Int] = Cachable.yes(2)
    val c3: Cachable[Int] = Cachable.yes(3)

    val x = for {
      a <- c1
      b <- c2
      c <- c3
    } yield (a + b + c)

    x should be(Cachable(6, true))
  }

  test("That constructors for both `Try` and others works as expected") {
    val t1                     = Success(1)
    val c1: Try[Cachable[Int]] = Cachable.yes(t1)
    c1 should be(Success(Cachable(1, true)))

    val t2                     = Success(2)
    val c2: Cachable[Try[Int]] = Cachable.yes(t2)
    c2 should be(Cachable(Success(2), true))
  }

  test("That merging returns non-cachable if any non-cachable") {
    val cachables1: List[Cachable[Int]] = List(Cachable.yes(1), Cachable.yes(2), Cachable.yes(3))
    val result1                         = Cachable.merge(cachables1)
    result1 should be(Cachable(List(1, 2, 3), true))

    val cachables2: List[Cachable[Int]] = List(Cachable.yes(1), Cachable.no(2), Cachable.yes(3))
    val result2                         = Cachable.merge(cachables2)
    result2 should be(Cachable(List(1, 2, 3), false))
  }
}
