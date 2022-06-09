package no.ndla.imageapi.service

trait Random {
  val random: Random
  class Random {
    def string(length: Int): String = {
      scala.util.Random.alphanumeric.take(length).mkString
    }
  }
}
