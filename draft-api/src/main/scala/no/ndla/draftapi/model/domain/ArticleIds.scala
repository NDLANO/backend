package no.ndla.draftapi.model.domain

case class ArticleIds(articleId: Long, externalId: List[String], importId: Option[String] = None)
