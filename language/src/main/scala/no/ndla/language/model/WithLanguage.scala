package no.ndla.language.model

trait WithLanguage extends Ordered[WithLanguage] {
  def compare(that: WithLanguage): Int = this.language.compare(that.language)
  def language: String
}
