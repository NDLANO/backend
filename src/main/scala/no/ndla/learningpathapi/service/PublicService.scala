package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.service.ModelConverters._
import no.ndla.learningpathapi._

class PublicService {
  val learningpathData = AmazonIntegration.getLearningpathData()

  def all(): List[LearningPathSummary] = {
    learningpathData.withStatus(LearningpathApiProperties.Published).map(asApiLearningpathSummary)
  }

  def withId(learningPathId: Long): Option[LearningPath] = {
    withIdAndAccessGranted(learningPathId).map(asApiLearningpath)
  }

  def statusFor(learningPathId: Long, owner:Option[String] = None): Option[LearningPathStatus] = {
    withIdAndAccessGranted(learningPathId).map(lp => LearningPathStatus(lp.status))
  }

  def learningstepsFor(learningPathId: Long): Option[List[LearningStep]] = {
    withIdAndAccessGranted(learningPathId) match {
      case Some(lp) => Some(learningpathData.learningStepsFor(lp.id.get).map(ls => asApiLearningStep(ls, lp)))
      case None => None
    }
  }

  def learningstepFor(learningPathId: Long, learningstepId: Long): Option[LearningStep] = {
    withIdAndAccessGranted(learningPathId) match {
      case Some(lp) => learningpathData.learningStepWithId(learningPathId, learningstepId).map(ls => asApiLearningStep(ls, lp))
      case None => None
    }
  }

  private def withIdAndAccessGranted(learningPathId: Long): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyPublic)
    learningPath
  }
}
