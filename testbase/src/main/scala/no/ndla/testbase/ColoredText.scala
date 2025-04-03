/*
 * Part of NDLA testbase
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.testbase

object ColoredText {
  val RED    = "\u001b[31m"
  val GREEN  = "\u001b[32m"
  val YELLOW = "\u001b[33m"
  val BLUE   = "\u001b[34m"
  val RESET  = "\u001b[0m"

  def print(color: Colors, text: String) = {
    color match {
      case Red    => println(s"$RED$text$RESET")
      case Green  => println(s"$GREEN$text$RESET")
      case Yellow => println(s"$YELLOW$text$RESET")
      case Blue   => println(s"$BLUE$text$RESET")
    }
  }

}
