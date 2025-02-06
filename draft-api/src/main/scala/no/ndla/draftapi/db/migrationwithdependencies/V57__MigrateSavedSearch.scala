/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, parser}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.draftapi.integration.{Node, TaxonomyApiClient}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.Props
import no.ndla.network.{AuthUser, NdlaClient, TaxonomyData}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, *}
import sttp.client3.StringBody
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

trait V57__MigrateSavedSearch {
  this: TaxonomyApiClient with NdlaClient with Props =>

  import props.{TaxonomyUrl, TaxonomyVersionHeader, Environment, auth0ManagmentClientId, auth0ManagmentClientSecret}

  class V57__MigrateSavedSearch extends BaseJavaMigration {

    val auth0Domain   = AuthUser.getAuth0HostForEnv(Environment)
    val managementUri = uri"https://$auth0Domain/oauth/token"
    val auth0Audience = s"https://$auth0Domain/api/v2/"

    case class Auth0TokenResponse(access_token: String)
    object Auth0TokenResponse {
      implicit val decoder: Decoder[Auth0TokenResponse] = deriveDecoder
    }

    case class Auth0AppMetadata(ndla_id: String)
    object Auth0AppMetadata {
      implicit val decoder: Decoder[Auth0AppMetadata] = deriveDecoder
    }
    case class Auth0Users(users: List[Auth0UserObject], length: Long, total: Long)
    object Auth0Users {
      implicit val decoder: Decoder[Auth0Users] = deriveDecoder
    }
    case class Auth0UserObject(name: String, app_metadata: Auth0AppMetadata)
    object Auth0UserObject {
      implicit val decoder: Decoder[Auth0UserObject] = deriveDecoder
    }
    case class GetTokenBody(client_id: String, client_secret: String, audience: String, grant_type: String)
    object GetTokenBody {
      implicit val encoder: Encoder[GetTokenBody] = deriveEncoder[GetTokenBody]
    }

    def getManagementToken(): Try[String] = {
      val inputBody = GetTokenBody(
        client_id = auth0ManagmentClientId,
        client_secret = auth0ManagmentClientSecret,
        audience = auth0Audience,
        grant_type = "client_credentials"
      )

      val jsonStr = CirceUtil.toJsonString(inputBody)
      val bod     = StringBody(jsonStr, "utf-8", sttp.model.MediaType.ApplicationJson)
      val req     = quickRequest.post(managementUri).body(bod)

      Try {
        val res = simpleHttpClient.send(req)
        CirceUtil
          .unsafeParseAs[Auth0TokenResponse](res.body)
          .access_token
      }
    }

    def fetchAuth0UsersByQuery(token: String, page: Long): Try[Auth0Users] = {
      val req = quickRequest
        .get(uri"https://$auth0Domain/api/v2/users?include_totals=true&q=app_metadata.isOrWasEdUser:true&page=${page}")
        .header("Authorization", s"Bearer $token")
        .header("content-type", "application/json")

      Try {
        val res = simpleHttpClient.send(req)
        CirceUtil
          .unsafeParseAs[Auth0Users](res.body)
      }
    }

    def getEditors(managementToken: String): Try[Map[String, Auth0UserObject]] = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(10))
      val firstPage     = fetchAuth0UsersByQuery(managementToken, 0).?
      val numberOfPages = Math.ceil(firstPage.total.toDouble / firstPage.length.toDouble)
      val users = (1 to numberOfPages.toInt).foldLeft(List[Future[Auth0Users]](Future(firstPage))) {
        case (acc, pageNumber) =>
          val x = Future(fetchAuth0UsersByQuery(managementToken, pageNumber.toLong).?)
          acc :+ x
      }
      val fut = Future.sequence(users)
      Try {
        val awaited = Await.result(fut, 10.minutes)
        awaited
          .flatMap(_.users)
          .map(x => x.app_metadata.ndla_id -> x)
          .toMap
      }
    }

    lazy val managementToken = getManagementToken().get
    lazy val auth0Editors    = getEditors(managementToken).get

    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val taxonomyTimeout     = 20.seconds

    def countAllRows(implicit session: DBSession): Option[Long] = {
      sql"select count(*) from userdata where document is not NULL"
        .map(rs => rs.long("count"))
        .single()
    }

    def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
      sql"select id, document, user_id from userdata where document is not null order by id limit 1000 offset $offset"
        .map(rs => {
          (rs.long("id"), rs.string("document"))
        })
        .list()
    }

    def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(document)

      sql"update userdata set document = $dataObject where id = $id"
        .update()
    }

    override def migrate(context: Context): Unit = DB(context.getConnection)
      .autoClose(false)
      .withinTx { session => migrateRows(session) }

    def migrateRows(implicit session: DBSession): Unit = {
      val count        = countAllRows.get
      var numPagesLeft = (count / 1000) + 1
      var offset       = 0L

      while (numPagesLeft > 0) {
        allRows(offset * 1000).map { case (id, document) =>
          updateRow(convertDocument(document), id)
        }: Unit
        numPagesLeft -= 1
        offset += 1
      }
    }
    private def get[A: Decoder](url: String, params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest
          .get(uri"$url".withParams(params: _*))
          .readTimeout(taxonomyTimeout)
          .header(TaxonomyVersionHeader, TaxonomyData.get),
        None
      )
    }

    private def getNode(id: String): Try[Node] = {
      get[Node](s"$TaxonomyApiEndpoint/nodes/$id") match {
        case Failure(_) =>
          Failure(api.NotFoundException(s"No topics with id $id"))
        case Success(node) => Try(node)
      }
    }

    private def getResourceType(id: String): Try[ResourceTypeMigration] =
      get[ResourceTypeMigration](s"$TaxonomyApiEndpoint/resource-types/$id") match {
        case Failure(_)            => { Failure(api.NotFoundException(s"No resource type with id $id")) }
        case Success(resourceType) => Try(resourceType)
      }

    def getStatusTranslation(status: String): String = {
      status match {
        case "PLANNED"           => "Planlagt"
        case "IN_PROGRESS"       => "I arbeid"
        case "EXTERNAL_REVIEW"   => "Eksternt gjennomsyn"
        case "INTERNAL_REVIEW"   => "Sisteblikk"
        case "QUALITY_ASSURANCE" => "Kvalitetssikring desk"
        case "LANGUAGE"          => "Språk"
        case "FOR_APPROVAL"      => "Godkjenning LMA"
        case "END_CONTROL"       => "Sluttkontroll"
        case "PUBLISH_DELAYED"   => "Publ-utsatt"
        case "PUBLISHED"         => "Publisert"
        case "REPUBLISH"         => "Til republisering"
        case "UNPUBLISHED"       => "Avpublisert"
        case "ARCHIVED"          => "Slettet"
      }
    }

    def convertDocument(document: String): String = {
      val oldUserData = parser.parse(document).flatMap(_.as[V55_UserData]).toTry.get

      val searchPhrases = oldUserData.savedSearches
        .getOrElse(Seq.empty)
        .map(s => {
          val parsed     = uri"$s"
          val paramsMap  = parsed.paramsMap
          val searchType = parsed.pathSegments.segments.last.v
          val searchPhrase = paramsMap.foldLeft("") {
            case (acc, ("query", v)) => s"$acc + $v"
            case (acc, ("subjects", v)) => {
              v match {
                case "urn:favourites"  => s"$acc + Mine favorittfag"
                case "urn:lmaSubjects" => s"$acc + Mine LMA-fag"
                case "urn:daSubjects"  => s"$acc + Mine DA-fag"
                case "urn:saSubjects"  => s"$acc + Mine SA-fag"
                case _ =>
                  getNode(v) match {
                    case Failure(_)    => acc
                    case Success(node) => s"$acc + ${node.name}"
                  }
              }
            }
            case (acc, ("resource-types", v)) => {
              v match {
                case "topic-article"     => s"$acc + Emne"
                case "frontpage-article" => s"$acc + Om-NDLA-artikkel"
                case _ =>
                  getResourceType(v) match {
                    case Failure(_)            => { acc }
                    case Success(resourceType) => s"$acc + ${resourceType.name}"
                  }
              }
            }
            case (acc, ("responsible-ids", v)) => {
              auth0Editors.get(v) match {
                case Some(user) => s"$acc + Ansvarlig: ${user.name}"
                case _          => acc
              }
            }
            case (acc, ("draft-status", v)) => s"$acc + ${getStatusTranslation(v)}"
            case (acc, ("users", v)) => {
              auth0Editors.get(v) match {
                case Some(user) => s"$acc + Bruker: ${user.name}"
                case _          => acc
              }
            }
            case (acc, ("language", v)) => {
              v match {
                case "en" => s"$acc + Engelsk"
                case "nn" => s"$acc + Nynorsk"
                case "nb" => s"$acc + Bokmål"
                case _    => acc
              }

            }
            case (acc, ("license", v)) => s"$acc + $v"
            case (acc, ("audio-type", v)) => {
              v match {
                case "standard" => s"$acc + Standard"
                case "podcast"  => s"$acc + Podcast"
                case _          => acc
              }
            }
            case (acc, ("model-released", v)) => {
              v match {
                case "yes"            => s"$acc + Modellklarert"
                case "no"             => s"$acc + Ikke modellklarert"
                case "not-set"        => s"$acc + Ikke valgt"
                case "not-applicable" => s"$acc + Ikke relevant"
              }
            }
            case (acc, ("concept-type", v)) => {
              v match {
                case "concept" => s"$acc + Forklaring"
                case "gloss"   => s"$acc + Glose"
                case _         => acc
              }
            }
            case (acc, ("status", v)) => s"$acc + ${getStatusTranslation(v)}"
            case (acc, ("filter-inactive", v)) => {
              v match {
                case "false" => s"$acc + Utgåtte fag inkludert"
                case _       => acc
              }
            }
            case (acc, ("exclude-revision-log", _))   => acc
            case (acc, ("fallback", _))               => acc
            case (acc, ("page-size", _))              => acc
            case (acc, ("sort", _))                   => acc
            case (acc, ("include-other-statuses", _)) => acc
            case (acc, ("revision-date-to", _))       => acc
            case (acc, ("revision-date-from", _))     => acc
            case (acc, ("page", _))                   => acc
            case (acc, ("types", _))                  => acc
            case (acc, (k, _)) =>
              println(s"Unhandled key: $k")
              acc
          }
          searchType match {
            case "content"        => s"Innhold$searchPhrase"
            case "audio"          => s"Lyd$searchPhrase"
            case "image"          => s"Bilde$searchPhrase"
            case "concept"        => s"Forklaring$searchPhrase"
            case "podcast-series" => s"Serie$searchPhrase"
            case _                => searchPhrase
          }

        })

      val newUserData = V57_UserData(
        userId = oldUserData.userId,
        savedSearches = oldUserData.savedSearches.map(el =>
          el.zipWithIndex.map { case (value, index) => V57_SavedSearch(value, searchPhrases(index)) }
        ),
        latestEditedArticles = oldUserData.latestEditedArticles,
        latestEditedConcepts = oldUserData.latestEditedConcepts,
        favoriteSubjects = oldUserData.favoriteSubjects
      )
      newUserData.asJson.noSpaces
    }
  }

  case class V57_SavedSearch(searchUrl: String, searchPhrase: String)
  object V57_SavedSearch {
    implicit def encoder: Encoder[V57_SavedSearch] = deriveEncoder
    implicit def decoder: Decoder[V57_SavedSearch] = deriveDecoder
  }
  case class V57_UserData(
      userId: String,
      savedSearches: Option[Seq[V57_SavedSearch]],
      latestEditedArticles: Option[Seq[String]],
      latestEditedConcepts: Option[Seq[String]],
      favoriteSubjects: Option[Seq[String]]
  )

  object V57_UserData {
    implicit val decoder: Decoder[V57_UserData] = deriveDecoder
    implicit val encoder: Encoder[V57_UserData] = deriveEncoder
  }

  case class V55_UserData(
      userId: String,
      savedSearches: Option[Seq[String]],
      latestEditedArticles: Option[Seq[String]],
      latestEditedConcepts: Option[Seq[String]],
      favoriteSubjects: Option[Seq[String]]
  )
  object V55_UserData {
    implicit def encoder: Encoder[V55_UserData] = deriveEncoder
    implicit def decoder: Decoder[V55_UserData] = deriveDecoder
  }

  case class TranslationMigration(
      name: String,
      language: String
  )
  object TranslationMigration {
    implicit def encoder: Encoder[TranslationMigration] = deriveEncoder
    implicit def decoder: Decoder[TranslationMigration] = deriveDecoder
  }

  case class ResourceTypeMigration(
      id: String,
      name: String,
      translations: Seq[TranslationMigration],
      supportedLanguages: Seq[String],
      subtypes: Option[Seq[ResourceTypeMigration]]
  )
  object ResourceTypeMigration {
    implicit def encoder: Encoder[ResourceTypeMigration] = deriveEncoder
    implicit def decoder: Decoder[ResourceTypeMigration] = deriveDecoder
  }
}
