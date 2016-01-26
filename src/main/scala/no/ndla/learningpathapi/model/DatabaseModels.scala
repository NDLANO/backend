package no.ndla.learningpathapi.model

import java.util.Date



case class LearningPath(id:Option[Long], title: List[Title], description: List[Description], learningsteps: List[LearningStep],
                        coverPhotoUrl: Option[String], duration: Int, status: String, lastUpdated: Date, owner: String)

case class LearningStep(id:Long, seqNo:Int, title:List[Title], embedUrl:List[EmbedUrl], `type`:String)
case class Title(title:String, language:Option[String])
case class EmbedUrl(url:String, language:Option[String])
case class Description(description:String, language:Option[String])




