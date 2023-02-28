package no.ndla.common.configuration

import no.ndla.common.Warmup

trait BaseComponentRegistry[PropType <: BaseProps] {
  implicit val props: PropType
  implicit val healthController: Warmup
}
