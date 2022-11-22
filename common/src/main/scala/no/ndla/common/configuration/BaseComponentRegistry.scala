package no.ndla.common.configuration

import no.ndla.common.Warmup

trait BaseComponentRegistry[PropType <: BaseProps] {
  val props: PropType
  val healthController: Warmup
}
