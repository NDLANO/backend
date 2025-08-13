/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

trait Random {
  lazy val random: Random
  class Random {
    def string(length: Int): String = {
      scala.util.Random.alphanumeric.take(length).mkString
    }
  }
}
