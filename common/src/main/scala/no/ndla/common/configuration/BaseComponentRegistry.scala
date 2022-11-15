package no.ndla.common.configuration

trait BaseComponentRegistry[PropType <: BaseProps] {
  val props: PropType
}
