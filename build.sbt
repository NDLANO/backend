import Dependencies.{testWith, versions}

ThisBuild / scalaVersion := versions.ScalaV

// format: off

// Modules / API's
lazy val `article-api`       = Module.setup(project in file("./article-api/"),      articleapi,      deps = Seq(network, mapping, language, validation, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `draft-api`         = Module.setup(project in file("./draft-api/"),        draftapi,        deps = Seq(network, mapping, language, validation, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `audio-api`         = Module.setup(project in file("./audio-api/"),        audioapi,        deps = Seq(network, mapping, language, common, search, testWith(scalatestsuite), database, testWith(tapirtesting)))
lazy val `concept-api`       = Module.setup(project in file("./concept-api/"),      conceptapi,      deps = Seq(network, mapping, language, validation, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `frontpage-api`     = Module.setup(project in file("./frontpage-api/"),    frontpageapi,    deps = Seq(network, mapping, language, common, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `image-api`         = Module.setup(project in file("./image-api/"),        imageapi,        deps = Seq(network, mapping, language, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `learningpath-api`  = Module.setup(project in file("./learningpath-api/"), learningpathapi, deps = Seq(network, mapping, language, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `oembed-proxy`      = Module.setup(project in file("./oembed-proxy/"),     oembedproxy,     deps = Seq(network, common, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `search-api`        = Module.setup(project in file("./search-api/"),       searchapi,       deps = Seq(network, mapping, language, common, search, database, testWith(scalatestsuite), testWith(tapirtesting)))
lazy val `myndla-api`        = Module.setup(project in file("./myndla-api/"),       myndlaapi,       deps = Seq(network, mapping, language, common, database, testWith(scalatestsuite), testWith(tapirtesting)))

// Libraries
lazy val testbase            = Module.setup(project in file("./testbase/"),          testbaselib)
lazy val scalatestsuite      = Module.setup(project in file("./scalatestsuite/"),    scalatestsuitelib, deps = Seq(common, testbase, database))
lazy val common              = Module.setup(project in file("./common/"),            commonlib,         deps = Seq(testWith(testbase), language))
lazy val tapirtesting        = Module.setup(project in file("./tapirtesting/"),      tapirtestinglib,   deps = Seq(common, network, scalatestsuite))
lazy val network             = Module.setup(project in file("./network/"),           networklib,        deps = Seq(common))
lazy val language            = Module.setup(project in file("./language/"),          languagelib)
lazy val mapping             = Module.setup(project in file("./mapping/"),           mappinglib)
lazy val validation          = Module.setup(project in file("./validation/"),        validationlib,     deps = Seq(common))
lazy val database            = Module.setup(project in file("./database/"),          databaselib,       deps = Seq(common, network))
lazy val search              = Module.setup(project in file("./search/"),            searchlib,         deps = Seq(testWith(scalatestsuite), language, common, mapping))
lazy val `integration-tests` = Module.setup(project in file("./integration-tests/"), integrationtests,  deps = Seq(testWith(validation, withTests = true), testWith(scalatestsuite), testWith(`article-api`, withTests = true), testWith(`draft-api`, withTests = true), testWith(`learningpath-api`, withTests = true), testWith(`search-api`, withTests = true)))
