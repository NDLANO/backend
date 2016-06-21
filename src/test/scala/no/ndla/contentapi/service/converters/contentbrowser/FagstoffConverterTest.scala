package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.integration.ContentFagstoff
import org.mockito.Mockito._

class FagstoffConverterTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleFagstoff1 = ContentFagstoff(nodeId, nodeId, "Tittel", "Innhold", "nb")
  val sampleFagstoff2 = ContentFagstoff(nodeId, nodeId2, "Tittel", "Innhald", "nn")

  test("That FagstoffConverter returns the contents of a fagstoff according to language") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeFagstoff(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    val (result, requiredLibraries, status) = FagstoffConverter.convert(content)

    result should equal (sampleFagstoff1.fagstoff)
    status.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That FagstoffConverter returns an error when the fagstoff is not found") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeFagstoff(nodeId)).thenReturn(Seq())
    val (result, requiredLibraries, status) = FagstoffConverter.convert(content)

    result should equal (s"{Import error: Failed to retrieve 'fagstoff' with language 'nb' ($nodeId)}")
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }
}