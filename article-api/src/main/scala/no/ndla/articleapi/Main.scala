package no.ndla.articleapi

import no.ndla.common.Environment.setPropsFromEnv

object Main {

  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val props = new ArticleApiProperties
    val mainClass = new MainClass(props)
    mainClass.start()
  }
}
