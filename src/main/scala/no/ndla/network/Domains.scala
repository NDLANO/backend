package no.ndla.network

object Domains {

  def get(environment: String): String = Map(
    "local" -> "http://api-gateway.ndla-local",
    "prod" -> "https://api.ndla.no"
  ).getOrElse(environment, s"https://$environment.api.ndla.no")


}
