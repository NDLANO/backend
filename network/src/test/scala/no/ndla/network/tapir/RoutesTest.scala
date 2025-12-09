/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.*
import sttp.client3.quick.simpleHttpClient
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*
import scala.util.Try

class RoutesTestController(using
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController {
  override val prefix: EndpointInput[Unit]               = "routes"
  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(abortRequestTestEndpoint)

  private def abortRequestTestEndpoint: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("aborted-request")
    .out(stringBody)
    .errorOut(errorOutputsFor(500))
    .serverLogicPure { _ =>
      Thread.sleep(1000)
      Right("Yippee!")
    }
}

class RoutesTest extends UnitTestSuite, TapirControllerTest {
  override implicit lazy val props: BaseProps = new BaseProps {
    override def ApplicationPort: Int    = findFreePort
    override def ApplicationName: String = "RoutesTest"
  }
  override implicit lazy val clock: Clock                 = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers   = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling = new ErrorHandling() {
    override def handleErrors: PartialFunction[Throwable, AllErrors] = PartialFunction.empty
  }
  implicit val myNDLAApiClient: MyNDLAApiClient              = mock[MyNDLAApiClient]
  override val controller: RoutesTestController              = new RoutesTestController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  def req: RequestT[Empty, Array[Byte], Any & Any] = basicRequest.response(asByteArrayAlways)

  private def getTapirRequestCounterDataPoints: Seq[CounterDataPointSnapshot] =
    routes.registry.scrape().iterator().asScala.find(_.getMetadata.getPrometheusName == "tapir_request") match {
      case Some(cs: CounterSnapshot) => cs.getDataPoints.asScala.toSeq
      case _                         => fail("Could not find tapir_request counter in Prometheus registry")
    }

  test("that aborting the request only increases the 499 error code metric") {
    Try(
      simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/routes/aborted-request").readTimeout(100.millis))
    ).failed.failIfFailure

    blockUntil(() => getTapirRequestCounterDataPoints.nonEmpty)

    inside(getTapirRequestCounterDataPoints) { case Seq(dataPoint) =>
      dataPoint.getLabels.get("status") should be("499")
      dataPoint.getValue should be(1.0d)
    }
  }
}
