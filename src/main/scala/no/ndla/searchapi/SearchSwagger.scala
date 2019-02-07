/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object SearchApiInfo {

  val apiInfo = ApiInfo(
    "A common endpoint for searching across article, draft, learningpath, image and audio APIs." +
      "The Search API provides a common endpoint for searching across the article, draft, learningpath, image and audio APIs. " +
      "The search does a free text search in data and metadata. It is also possible to search targeted at specific " +
      "meta-data fields like language or license.\n" +
      "Note that the query parameter is based on the Elasticsearch simple search language. For more information, see " +
      "https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html",
    "Documentation for the Search API of NDLA.no",
    "https://om.ndla.no/terms",
    SearchApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )
}

class SearchSwagger extends Swagger("2.0", "0.8", SearchApiInfo.apiInfo) {
  addAuthorization(
    OAuth(List(), List(ImplicitGrant(LoginEndpoint(SearchApiProperties.Auth0LoginEndpoint), "access_token"))))
}
