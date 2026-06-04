/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import org.springframework.stereotype.Component

/**
 * Used to ensure that queries for old style URLS of various kinds (ndla.no/node/xxx) are
 * transformed to the same format, used in lookup.
 */
@Component
class OldUrlCanonifier {
  fun canonify(oldUrl: String): String {
    val url = discardKnownNodeSuffixes(replaceKnownNodePrefixes(oldUrl))
    var nodeId = ""
    var fagId = ""
    for (token in url.split("?").filter { it.isNotEmpty() }) {
      if (!token.contains("=")) {
        val nodeStartsAt = token.substring(0, token.lastIndexOf('/')).lastIndexOf('/')
        nodeId = token.substring(nodeStartsAt)
      } else if (token.contains("fag=")) {
        val start = token.indexOf("fag=")
        val ampersandIndex = token.indexOf("&")
        val end = if (ampersandIndex > start) ampersandIndex else token.length
        fagId = "?" + token.substring(start, end)
      }
    }
    return "ndla.no$nodeId$fagId"
  }

  private fun discardKnownNodeSuffixes(oldUrl: String): String {
    val suffix = KNOWN_NODE_SUFFIXES.firstOrNull { oldUrl.contains(it) }
    if (suffix == null) return oldUrl
    val start = oldUrl.indexOf(suffix)
    val indexOfSlashAfter = oldUrl.indexOf('/', start + 1)
    val indexOfQuestionMark = oldUrl.indexOf('?', start)
    val partToRemove =
        if (indexOfSlashAfter != -1) {
          oldUrl.substring(start, indexOfSlashAfter + 1)
        } else if (indexOfQuestionMark != -1) {
          oldUrl.substring(start, indexOfQuestionMark)
        } else suffix
    return oldUrl.replace(partToRemove, "")
  }

  private fun replaceKnownNodePrefixes(oldUrl: String) =
      KNOWN_NODE_PREFIXES.firstOrNull { oldUrl.contains(it) }?.let { oldUrl.replace(it, "node") }
          ?: oldUrl

  companion object {
    private val KNOWN_NODE_PREFIXES =
        listOf(
            "printpdf",
            "easyreader",
            "h5p/embed",
            "h5pcontent",
            "aktualitet",
            "package",
            "fagstoff",
            "oppgave",
            "print",
        )
    private val KNOWN_NODE_SUFFIXES = listOf("/menu", "/oembed", "/download", "/lightbox")
  }
}
