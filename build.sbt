import Dependencies.{testWith, versions}

ThisBuild / scalaVersion := versions.ScalaV

// Modules / API's
lazy val `article-api`: Project = Module.setup(
  project in file("./article-api/"),
  articleapi,
  deps = Seq(
    network,
    mapping,
    language,
    validation,
    common,
    search,
    testWith(scalatestsuite)
  )
)

lazy val `draft-api`: Project = Module.setup(
  project in file("./draft-api/"),
  draftapi,
  deps = Seq(
    network,
    mapping,
    language,
    validation,
    common,
    search,
    testWith(scalatestsuite)
  )
)

lazy val `audio-api` = Module.setup(
  project in file("./audio-api/"),
  audioapi,
  deps = Seq(network, mapping, language, common, search, testWith(scalatestsuite))
)

lazy val `concept-api` = Module.setup(
  project in file("./concept-api/"),
  conceptapi,
  deps = Seq(network, mapping, language, validation, common, search, testWith(scalatestsuite))
)

lazy val `frontpage-api` = Module.setup(
  project in file("./frontpage-api/"),
  frontpageapi,
  deps = Seq(network, mapping, language, common, testWith(scalatestsuite))
)

lazy val `image-api` = Module.setup(
  project in file("./image-api/"),
  imageapi,
  deps = Seq(network, mapping, language, common, search, testWith(scalatestsuite))
)

lazy val `learningpath-api` = Module.setup(
  project in file("./learningpath-api/"),
  learningpathapi,
  deps = Seq(network, mapping, language, common, search, testWith(scalatestsuite))
)

lazy val `oembed-proxy` = Module.setup(project in file("./oembed-proxy/"), oembedproxy, deps = Seq(network, common))

lazy val `search-api` = Module.setup(
  project in file("./search-api/"),
  searchapi,
  deps = Seq(network, mapping, language, common, search, testWith(scalatestsuite))
)

// Libraries
lazy val common         = Module.setup(project in file("./common/"), commonlib, deps = Seq(testWith(scalatestsuite)))
lazy val scalatestsuite = Module.setup(project in file("./scalatestsuite/"), scalatestsuitelib, deps = Seq(network))
lazy val network        = Module.setup(project in file("./network/"), networklib)
lazy val language       = Module.setup(project in file("./language/"), languagelib)
lazy val mapping        = Module.setup(project in file("./mapping/"), mappinglib)
lazy val validation     = Module.setup(project in file("./validation/"), validationlib)
lazy val search         = Module.setup(project in file("./search/"), searchlib, deps = Seq(language, common))

lazy val `integration-tests` = Module.setup(
  project in file("./integration-tests/"),
  integrationtests,
  deps = Seq(
    testWith(scalatestsuite),
    testWith(`article-api`, withTests = true),
    testWith(`draft-api`, withTests = true),
    testWith(`learningpath-api`, withTests = true),
    testWith(`search-api`, withTests = true)
  )
)
