package no.ndla.language.model

trait LanguageField[T] extends WithLanguageAndValue[T] {
  def value: T
  def isEmpty: Boolean
}
