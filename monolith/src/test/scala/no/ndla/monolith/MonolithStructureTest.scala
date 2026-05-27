/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.network.tapir.LegacyPrefixAlias
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

  test("That LegacyPrefixAlias controllers are dropped from the merged monolith services") {
    // Each per-app CR registers a LegacyPrefixAlias of its InternController at the bare `intern` prefix to keep
    // microservice-mode callers working during the per-app prefix migration. The monolith must drop these because
    // their `intern/*` paths would collide across apps. Pin the filter so a regression surfaces here, not as a
    // production shadowing bug.
    val cr      = new MonolithComponentRegistry(new MonolithProperties)
    val aliases = cr.services.filter(_.isInstanceOf[LegacyPrefixAlias])
    aliases shouldBe empty
  }

  test("That no endpoints are shadowed across the merged per-app controllers") {
    // Per-controller TapirControllerTest catches shadowing inside a single controller. This is the application-wide
    // equivalent: every per-app CR's controllers are merged into one Netty server in monolith mode, so collisions
    // across apps (e.g. nine `/intern` prefixes) only surface here.
    //
    // Forces every per-app CR via `cr.services`. DataSource and Elastic4sClient are constructed lazily inside each
    // CR, so this does not open a DB connection or hit Elasticsearch.
    val cr           = new MonolithComponentRegistry(new MonolithProperties)
    val allEndpoints = cr.services.flatMap(_.builtEndpoints.map(_.endpoint))
    val errors       = sttp.tapir.testing.EndpointVerifier(allEndpoints)
    if (errors.nonEmpty) {
      val errString = errors.map(_.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Found shadowed endpoints across merged monolith controllers:$errString")
    }
  }
}
