package no.ndla.contentapi.batch.service.integration

import no.ndla.contentapi.batch.service.ConverterModules

import scala.collection.JavaConversions._
import org.jsoup.nodes.Element

object ContentBrowserConverter extends ConverterModule {
  case class ContentBrowser(contentBrowserString: String) {
    // Extract the contentbrowser variables
    private val Pattern = """.*\[contentbrowser (.*) contentbrowser\].*""".r
    private val ContentField = contentBrowserString match {
      case Pattern(group) => group
      case _ => ""
    }

    // Extract every key-value pair and build a map
    private val KeyVal = ContentField.split("==").map(x => x.stripPrefix("=").split("="))
    private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap


    def isContentBrowserField(): Boolean = {
      contentBrowserString.matches(Pattern.toString)
    }

    def getStartEndIndex(): (Int, Int) = {
      (Pattern findFirstMatchIn contentBrowserString) match {
        case Some(value) => (value.start, value.end)
        case None => (0, 0)
      }
    }

    def get(key: String): Option[String] = {
      FieldMap.get(key)
    }
  }

  def convert(doc: Element) {
    val elements = doc.select("p")

    for (el <- elements) {
      val text = el.text
      val cont = ContentBrowser(text)

      if (cont.isContentBrowserField()) {
        val (start, end) = cont.getStartEndIndex()
        val newContent = text.substring(0, start) + s"{CONTENT-${cont.get("nid").get}}" + text.substring(end)
        el.html(newContent)
      }
    }
  }
}
