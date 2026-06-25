/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util

import no.ndla.taxonomy.domain.LanguageField
import no.ndla.taxonomy.domain.NodeType
import org.jsoup.Jsoup

object PrettyUrlUtil {

  fun createPrettyUrl(
      rootName: LanguageField<String>?,
      name: LanguageField<String>?,
      language: String,
      hash: String?,
      nodeType: NodeType,
  ): String? =
      createPrettyUrl(
          rootName?.fromLanguage(language),
          name?.fromLanguage(language),
          hash,
          nodeType,
      )

  fun createPrettyUrl(
      rootName: String?,
      name: String?,
      hash: String?,
      nodeType: NodeType,
  ): String? {
    if (name == null || hash == null) return null
    return buildString {
      append(nodeTypeMapping(nodeType))
      append("/")
      if (rootName != null && rootName != name) {
        append(buildUrlFragment(cleanString(rootName)))
        append("/")
      }
      append(buildUrlFragment(cleanString(name)))
      append("/$hash")
    }
  }

  fun prettyName(name: String) = buildUrlFragment(cleanString(name))

  private fun buildUrlFragment(text: String): String =
      text.split(Regex("""\s+""")).joinToString("-")

  private fun nodeTypeMapping(nodeType: NodeType) =
      when (nodeType) {
        NodeType.SUBJECT -> "/f"
        NodeType.TOPIC,
        NodeType.CASE -> "/e"
        NodeType.RESOURCE -> "/r"
        NodeType.PROGRAMME -> "/utdanning"
        NodeType.NODE -> ""
      }

  private val SPECIAL_CHARACTERS_PATTERN = Regex("""[.,!?¿()/«»"”“'¡:`’#°π€%&|…]""")
  private val DASH_PATTERN = Regex("""[—–−_：，]""")

  private val AA_PATTERN = Regex("""[æååäáǎāàãâ]""")
  private val O_PATTERN = Regex("""[øöôóòõǒ]""")
  private val I_PATTERN = Regex("""[ïíǐīìĩ]""")
  private val E_PATTERN = Regex("""[éèẽêë]""")
  private val S_PATTERN = Regex("""[šŝśş]""")
  private val C_PATTERN = Regex("""[čĉć]""")
  private val U_PATTERN = Regex("""[üùũúû]""")
  private val D_PATTERN = Regex("""[ðđ]""")

  private fun cleanString(name: String) =
      Jsoup.parse(name)
          .text()
          .lowercase()
          .replace(SPECIAL_CHARACTERS_PATTERN, "")
          .replace(AA_PATTERN, "a")
          .replace(O_PATTERN, "o")
          .replace(I_PATTERN, "i")
          .replace(E_PATTERN, "e")
          .replace(S_PATTERN, "s")
          .replace(C_PATTERN, "c")
          .replace(U_PATTERN, "u")
          .replace(D_PATTERN, "d")
          .replace("ğ", "g")
          .replace("ñ", "n")
          .replace("ž", "z")
          .replace("ß", "ss")
          .replace("₂", "2")
          .replace(DASH_PATTERN, "-")
          .trim()

  fun getHashFromPath(title: String): String {
    if (title.contains("__")) {
      return title.split("__").getOrNull(1) ?: ""
    }

    // titles with hash will have a /r/ or /e/ or /f/ or /utdanning/ in the path
    if (!title.contains("/r/") &&
        !title.contains("/e/") &&
        !title.contains("/f/") &&
        !title.contains("/utdanning/")) {
      return ""
    }

    return title.substringAfterLast('/')
  }
}
