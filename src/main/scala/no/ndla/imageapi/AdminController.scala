package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.AmazonIntegration
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.NativeJsonSupport

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  val meta = AmazonIntegration.getImageMeta()
  val search = AmazonIntegration.getSearchMeta()
  val searchAdmin = AmazonIntegration.getSearchAdmin(search)

  def indexDocuments() = {
    val start = System.currentTimeMillis()

    val prevIndex = searchAdmin.usedIndex
    val index = prevIndex + 1
    logger.info(s"Indexing ${meta.elements.length} documents into index $index")
    searchAdmin.createIndex(index)
    searchAdmin.indexDocuments(meta.elements, index)
    searchAdmin.useIndex(index)
    if(prevIndex > 0) {
      searchAdmin.deleteIndex(prevIndex)
    }
    
    val result = s"Indexing took ${System.currentTimeMillis() - start} ms."
    logger.info(result)
  }

  post("/index") {
    indexDocuments()
  }
}
