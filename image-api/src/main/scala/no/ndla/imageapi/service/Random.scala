package no.ndla.imageapi.service

import scala.util.Random

trait Random {
  val random: Random
  class Random {
    def string(length: Int): String = {
      Random.alphanumeric.take(length).mkString
    }
  }
}
