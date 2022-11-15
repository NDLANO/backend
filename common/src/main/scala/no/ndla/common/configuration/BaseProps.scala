package no.ndla.common.configuration

import scala.util.Properties.propOrElse

trait BaseProps {
  def ApplicationPort: Int
  def ApplicationName: String
  def Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
}
