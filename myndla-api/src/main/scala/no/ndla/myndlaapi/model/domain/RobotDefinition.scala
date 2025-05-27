/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits.OptionImplicit
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.model.api.robot.RobotConfigurationDTO
import no.ndla.network.model.FeideID
import scalikejdbc.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class RobotDefinition(
    id: UUID,
    feideId: FeideID,
    status: RobotStatus,
    configuration: RobotConfiguration,
    created: NDLADate,
    updated: NDLADate,
    shared: Option[NDLADate]
) {
  def canEdit(feideId: String): Try[RobotDefinition] = {
    if (this.feideId == feideId) Success(this)
    else Failure(new AccessDeniedException("You do not have access to edit this robot definition"))
  }

  def canRead(feideId: FeideID, notFound: Boolean): Try[RobotDefinition] = {
    if (this.feideId == feideId || this.status == RobotStatus.SHARED) Success(this)
    else if (notFound) Failure(NotFoundException(s"Robot definition with id $id not found"))
    else Failure(new AccessDeniedException("You do not have access to read this robot definition"))
  }
}

case class RobotConfiguration(
    title: String,
    version: String,
    settings: RobotSettings
)
case class RobotSettings(
    name: String,
    systemprompt: Option[String],
    question: Option[String],
    temperature: String,
    model: String
)

object RobotSettings {
  implicit val encoder: Encoder[RobotSettings] = deriveEncoder[RobotSettings]
  implicit val decoder: Decoder[RobotSettings] = deriveDecoder[RobotSettings]
}

object RobotConfiguration {
  implicit val encoder: Encoder[RobotConfiguration] = deriveEncoder[RobotConfiguration]
  implicit val decoder: Decoder[RobotConfiguration] = deriveDecoder[RobotConfiguration]

  def fromDTO(dto: RobotConfigurationDTO): RobotConfiguration = {
    RobotConfiguration(
      title = dto.title,
      version = dto.version,
      settings = RobotSettings(
        name = dto.settings.name,
        systemprompt = dto.settings.systemprompt,
        question = dto.settings.question,
        temperature = dto.settings.temperature,
        model = dto.settings.model
      )
    )
  }

}

object RobotDefinition extends SQLSyntaxSupport[RobotDefinition] {
  override val tableName: String = "robot_definitions"

  def fromResultSet(sp: SyntaxProvider[RobotDefinition])(rs: WrappedResultSet): Try[RobotDefinition] = {
    val wrapper: String => String = (s: String) => sp.resultName.c(s)
    fromResultSet(wrapper)(rs)
  }

  def fromResultSet(rs: WrappedResultSet): Try[RobotDefinition] = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[RobotDefinition] = {
    import no.ndla.myndlaapi.uuidBinder
    val id        = rs.get[Try[UUID]](colNameWrapper("id"))
    val feideId   = rs.string(colNameWrapper("feide_id"))
    val statusStr = rs.string(colNameWrapper("status"))
    val status = RobotStatus
      .withNameOption(statusStr)
      .toTry(new IllegalArgumentException(s"Invalid robot status: $statusStr"))
    val created       = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("created")))
    val updated       = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("updated")))
    val shared        = rs.localDateTimeOpt(colNameWrapper("shared")).map(NDLADate.fromUtcDate)
    val configuration = CirceUtil.tryParseAs[RobotConfiguration](rs.string(colNameWrapper("configuration")))

    for {
      id     <- id
      status <- status
      config <- configuration
    } yield RobotDefinition(
      id = id,
      feideId = feideId,
      status = status,
      created = created,
      updated = updated,
      shared = shared,
      configuration = config
    )
  }

}
