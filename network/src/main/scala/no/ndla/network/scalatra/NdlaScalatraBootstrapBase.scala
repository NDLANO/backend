/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.scalatra

import org.scalatra.LifeCycle
import javax.servlet.ServletContext

abstract class NdlaScalatraBootstrapBase[T] extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    val componentRegistry = context.getAttribute("ComponentRegistry").asInstanceOf[T]
    ndlaInit(context, componentRegistry)
  }

  def ndlaInit(context: ServletContext, componentRegistry: T): Unit
}
