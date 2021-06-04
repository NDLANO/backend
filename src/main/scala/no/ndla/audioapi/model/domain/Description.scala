package no.ndla.audioapi.model.domain

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String = description
}
