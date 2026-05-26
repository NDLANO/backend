/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.testbase.UnitTestSuiteBase

import scala.util.Properties.{clearProp, propOrNone, setProp}

/** Structural smoke test that exercises the monolith wiring without booting any external infrastructure. Each per-app
  * `ComponentRegistry` is a `lazy val` inside [[MonolithComponentRegistry]], so simply constructing the registry does
  * not force the per-app CRs (and thus does not require a Postgres / Elasticsearch). The full boot-the-server smoke
  * test that hits every app's endpoints is intentionally left for a later testcontainers-backed setup.
  */
class MonolithStructureTest extends UnitTestSuiteBase {

  private def withProp[A](key: String, value: String)(body: => A): A = {
    val previous = propOrNone(key)
    setProp(key, value)
    try body
    finally previous match {
        case Some(p) => setProp(key, p): Unit
        case None    => clearProp(key): Unit
      }
  }

  test("MonolithProperties exposes one props instance per Scala *-api in the repo") {
    val props = new MonolithProperties
    props.article.ApplicationName should be("article-api")
    props.audio.ApplicationName should be("audio-api")
    props.concept.ApplicationName should be("concept-api")
    props.draft.ApplicationName should be("draft-api")
    props.frontpage.ApplicationName should be("frontpage-api")
    props.image.ApplicationName should be("image-api")
    props.learningpath.ApplicationName should be("learningpath-api")
    props.myndla.ApplicationName should be("myndla-api")
    props.oembed.ApplicationName should be("oembed-proxy")
    props.search.ApplicationName should be("search-api")
  }

  test("MonolithProperties uses a single APPLICATION_PORT for the merged server") {
    withProp("APPLICATION_PORT", "12345") {
      new MonolithProperties().ApplicationPort should be(12345)
    }
  }

  test("MonolithComponentRegistry can be instantiated without forcing any per-app CR") {
    // If this compiles and runs, the orchestration wiring is structurally sound. No per-app CR is touched here, so
    // no DataSource is opened and no Elasticsearch client is created.
    val cr = new MonolithComponentRegistry(new MonolithProperties)
    cr should not be null
    cr.warmupEndpoints.map(_._1) should contain allOf ("/article-api/v2/articles", "/draft-api/v1/drafts/", "/health")
  }
}
