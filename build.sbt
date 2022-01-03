import Dependencies.common

ThisBuild / scalaVersion := common.ScalaV

// format: off
lazy val `article-api`      = Module.setup(project in file("./article-api/"), articleapi)
lazy val `draft-api`        = Module.setup(project in file("./draft-api/"), draftapi)
lazy val `audio-api`        = Module.setup(project in file("./audio-api/"), audioapi)
lazy val `concept-api`      = Module.setup(project in file("./concept-api/"), conceptapi)
lazy val `frontpage-api`    = Module.setup(project in file("./frontpage-api/"), frontpageapi)
lazy val `image-api`        = Module.setup(project in file("./image-api/"), imageapi)
lazy val language           = Module.setup(project in file("./language/"), languagelib)
lazy val `learningpath-api` = Module.setup(project in file("./learningpath-api/"), learningpathapi)
lazy val mapping            = Module.setup(project in file("./mapping/"), mappinglib)
lazy val network            = Module.setup(project in file("./network/"), networklib)
lazy val `oembed-proxy`     = Module.setup(project in file("./oembed-proxy/"), oembedproxy)
lazy val scalatestsuite     = Module.setup(project in file("./scalatestsuite/"), scalatestsuitelib)
lazy val `search-api`       = Module.setup(project in file("./search-api/"), searchapi)
lazy val validation         = Module.setup(project in file("./validation/"), validationlib)
