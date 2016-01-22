package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.service.{PrivateLearningpathsService, PublicLearningpathsService}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

class LearningpathController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  protected val applicationDescription = "API for accessing Learningpaths from ndla.no."
  val getLearningpaths =
    (apiOperation[List[LearningpathSummary]]("getLearningpaths")
      summary "Show all learningpaths"
      notes "Shows all the learningpaths."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("query").description("Return only Learningpaths's with content matching the specified query."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
      queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
      )
      )

  val getLearningpath =
    (apiOperation[Learningpath]("getLearningpath")
      summary "Show details about the specified learningpath"
      notes "Shows all information about the specified learningpath."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningpathStatus =
    (apiOperation[LearningpathStatus]("getLearningpathStatus")
      summary "Show publishingstatus for the learningpath"
      notes "Shows publishingstatus for the learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningsteps =
    (apiOperation[List[Learningstep]]("getLearningsteps")
      summary "Show all learningsteps for given learningpath id"
      notes "Show all learningsteps for given learningpath id"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningstep =
    (apiOperation[Learningstep]("getLearningstep")
      summary "Show the given learningstep for the given learningpath"
      notes "Show the given learningstep for the given learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath."),
      queryParam[Option[String]]("step_id").description("The id of the learningpath.")
      )
      )

  val addNewLearningpath =
    (apiOperation[Learningpath]("addLearningpath")
      summary "Adds the given learningpath"
      notes "Adds the given learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      bodyParam[Learningpath]
      )
      )

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear()
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage)
      halt(status = 500, body = Error.GenericError)
    }
  }

  val publics = new PublicLearningpathsService()
  val privates = new PrivateLearningpathsService()

  // PUBLIC GET
  get("/", operation(getLearningpaths)) {
    publics.all()
  }

  get("/:path_id", operation(getLearningpath)) {
    publics.withId(params.get("path_id"))
  }

  get("/:path_id/status", operation(getLearningpathStatus)) {
    publics.statusFor(params.get("path_id"))
  }

  get("/:path_id/learningsteps", operation(getLearningsteps)) {
    publics.learningstepsFor(params.get("path_id"))
  }

  get("/:path_id/learningsteps/:step_id", operation(getLearningstep)) {
    publics.learningstepFor(params.get("path_id"), params.get("step_id"))
  }

  // PRIVATE GET
  get("/private", operation(getLearningpaths)) {
    privates.all()
  }

  get ("/private/:path_id", operation(getLearningpath)){
    privates.withId(params.get("path_id"))
  }

  get("/private/:path_id/status", operation(getLearningpathStatus)) {
    privates.statusFor(params.get("path_id"))
  }

  get("/private/:path_id/learningsteps", operation(getLearningsteps)) {
    privates.learningstepsFor(params.get("path_id"))
  }

  get("/private/:path_id/learningsteps/:step_id", operation(getLearningstep)) {
    privates.learningstepFor(params.get("path_id"), params.get("step_id"))
  }

  // ADD ELEMENTS
  post("/", operation(addNewLearningpath)) {
    logger.info(s"ADD LEARNINGPATH = ${request.body}")
    Ok(body = request.body)
  }

  post("/:path_id/learningsteps/") {
    logger.info(s"ADD LEARNINGSTEP = ${request.body}")
    Ok(body = request.body)
  }

  // UPDATE ELEMENTS
  put("/:path_id/") {
    logger.info(s"UPDATE LEARNINGPATH = ${request.body}")
    Ok(body = request.body)
  }

  put("/:path_id/learningsteps/:step_id/") {
    logger.info(s"UPDATE LEARNINGSTEP = ${request.body}")
    Ok(body = request.body)
  }

  put("/:path_id/status/") {
    logger.info(s"UPDATE PUBLISHSTATUS = ${request.body}")
    Ok(body = request.body)
  }

  // DELETE ELEMENTS
  delete("/:path_id") {
    logger.info(s"DELETE LEARNINGPATH ID: ${params.get("path_id")}")
    halt(status = 204)
  }

  delete("/:path_id/learningsteps/:step_id") {
    logger.info(s"DELETE LEARNINGPATH ID: ${params.get("path_id")} AND STEP ID: ${params.get("step_id")}")
    halt(status = 204)
  }


}
