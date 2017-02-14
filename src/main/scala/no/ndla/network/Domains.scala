package no.ndla.network

object Domains {

  def get(environment: String): String = Map(
    "local" -> "http://proxy.ndla-local",
    "prod" -> "https://api.ndla.no"
  ).getOrElse(environment, s"http://$environment.api.ndla.no")


}
