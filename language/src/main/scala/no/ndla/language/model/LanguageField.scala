package no.ndla.language.model

trait LanguageField[T] extends WithLanguage {
  def value: T
  def isEmpty: Boolean
}
