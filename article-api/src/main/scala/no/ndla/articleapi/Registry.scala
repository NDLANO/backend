package no.ndla.articleapi

import no.ndla.articleapi.service.search.{ArticleIndexService, SearchConverterService}
import no.ndla.search.SearchLanguage

class Registry(using props: ArticleApiProperties) {
  given searchLanguage         = new SearchLanguage()
  given searchConverterService = new SearchConverterService()
  given articleIndexService    = new ArticleIndexService()
}
