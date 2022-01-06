import Dependencies.versions

ThisBuild / scalaVersion := versions.ScalaV

// format: off
lazy val `article-api`      = Module.setup(project in file("./article-api/"),      articleapi,        deps = Seq(network, mapping, language, validation, scalatestsuite % "test", common, search))
lazy val `draft-api`        = Module.setup(project in file("./draft-api/"),        draftapi,          deps = Seq(network, mapping, language, validation, scalatestsuite % "test", common, search))
lazy val `audio-api`        = Module.setup(project in file("./audio-api/"),        audioapi,          deps = Seq(network, mapping, language,             scalatestsuite % "test", common, search))
lazy val `concept-api`      = Module.setup(project in file("./concept-api/"),      conceptapi,        deps = Seq(network, mapping, language, validation, scalatestsuite % "test", common, search))
lazy val `frontpage-api`    = Module.setup(project in file("./frontpage-api/"),    frontpageapi,      deps = Seq(network, mapping,                       scalatestsuite % "test", common))
lazy val `image-api`        = Module.setup(project in file("./image-api/"),        imageapi,          deps = Seq(network, mapping, language,             scalatestsuite % "test", common, search))
lazy val `learningpath-api` = Module.setup(project in file("./learningpath-api/"), learningpathapi,   deps = Seq(network, mapping, language,             scalatestsuite % "test", common, search))
lazy val `oembed-proxy`     = Module.setup(project in file("./oembed-proxy/"),     oembedproxy,       deps = Seq(network,                                                         common))
lazy val `search-api`       = Module.setup(project in file("./search-api/"),       searchapi,         deps = Seq(network, mapping, language,             scalatestsuite % "test", common, search))

lazy val common             = Module.setup(project in file("./common/"),           commonlib)
lazy val scalatestsuite     = Module.setup(project in file("./scalatestsuite/"),   scalatestsuitelib, deps = Seq(network))
lazy val network            = Module.setup(project in file("./network/"),          networklib)
lazy val language           = Module.setup(project in file("./language/"),         languagelib)
lazy val mapping            = Module.setup(project in file("./mapping/"),          mappinglib)
lazy val validation         = Module.setup(project in file("./validation/"),       validationlib)
lazy val search             = Module.setup(project in file("./search/"),           searchlib)
