/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.util.Success

class SearchServiceTest extends UnitSuite with TestEnvironment {

  override val searchService = new SearchService

  test("That createEmptyIndexIfNoIndexesExist never creates empty index if an index already exists") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(Seq("index1")))
    searchService.createEmptyIndexIfNoIndexesExist()
    verify(imageIndexService, never).createIndexWithName(any[String])
  }

  test("That createEmptyIndexIfNoIndexesExist creates empty index if no indexes already exists") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(List.empty))
    when(imageIndexService.createIndexWithGeneratedName).thenReturn(Success("images-123j"))
    searchService.createEmptyIndexIfNoIndexesExist()
    verify(imageIndexService).createIndexWithGeneratedName
  }

}
