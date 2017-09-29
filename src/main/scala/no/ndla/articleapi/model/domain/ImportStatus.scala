/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.domain

case class ImportStatus(messages: Seq[String], visitedNodes: Seq[String] = Seq(), articleId: Option[Long] = None) {
  def ++(importStatus: ImportStatus): ImportStatus =
    ImportStatus(messages ++ importStatus.messages, visitedNodes ++ importStatus.visitedNodes, importStatus.articleId)
  def addMessage(message: String): ImportStatus = this.copy(messages = this.messages :+ message)
  def setArticleId(id: Long) = this.copy(articleId = Some(id))
}

object ImportStatus {
  def empty = ImportStatus(Seq.empty, Seq.empty, None)
  def apply(message: String, visitedNodes: Seq[String]): ImportStatus = ImportStatus(Seq(message), visitedNodes, None)
  def apply(importStatuses: Seq[ImportStatus]): ImportStatus = {
    val (messages, visitedNodes, articleIds) = importStatuses.map(x => (x.messages, x.visitedNodes, x.articleId)).unzip3
    ImportStatus(messages.flatten.distinct, visitedNodes.flatten.distinct, articleIds.lastOption.flatten)
  }

}
