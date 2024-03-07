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
    testWith(scalatestsuite),
    testWith(tapirtesting)
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
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `audio-api` = Module.setup(
  project in file("./audio-api/"),
  audioapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    search,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `concept-api` = Module.setup(
  project in file("./concept-api/"),
  conceptapi,
  deps = Seq(
    network,
    mapping,
    language,
    validation,
    common,
    search,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `frontpage-api` = Module.setup(
  project in file("./frontpage-api/"),
  frontpageapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `image-api` = Module.setup(
  project in file("./image-api/"),
  imageapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    search,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `learningpath-api` = Module.setup(
  project in file("./learningpath-api/"),
  learningpathapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    search,
    myndla,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `oembed-proxy` = Module.setup(
  project in file("./oembed-proxy/"),
  oembedproxy,
  deps = Seq(network, common, testWith(scalatestsuite), testWith(tapirtesting))
)

lazy val `search-api` = Module.setup(
  project in file("./search-api/"),
  searchapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    search,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val `myndla-api` = Module.setup(
  project in file("./myndla-api/"),
  myndlaapi,
  deps = Seq(
    network,
    mapping,
    language,
    common,
    myndla,
    testWith(scalatestsuite),
    testWith(tapirtesting)
  )
)

lazy val constants = Module.setup(
  project,
  constantslib,
  deps = Seq(
    common,
    network,
    language,
    mapping,
    myndla,
    `concept-api`,
    testWith(scalatestsuite)
  )
)

// Libraries
lazy val common = Module.setup(project in file("./common/"), commonlib, deps = Seq(testWith(scalatestsuite), language))
lazy val scalatestsuite = Module.setup(project in file("./scalatestsuite/"), scalatestsuitelib)
lazy val tapirtesting =
  Module.setup(project in file("./tapirtesting/"), tapirtestinglib, deps = Seq(common, network, scalatestsuite))
lazy val network    = Module.setup(project in file("./network/"), networklib, deps = Seq(common))
lazy val language   = Module.setup(project in file("./language/"), languagelib)
lazy val mapping    = Module.setup(project in file("./mapping/"), mappinglib)
lazy val validation = Module.setup(project in file("./validation/"), validationlib, deps = Seq(common))
lazy val search =
  Module.setup(project in file("./search/"), searchlib, deps = Seq(testWith(scalatestsuite), language, common, mapping))
lazy val myndla =
  Module.setup(project in file("./myndla/"), myndlalib, deps = Seq(common, network, testWith(scalatestsuite)))

lazy val `integration-tests` = Module.setup(
  project in file("./integration-tests/"),
  integrationtests,
  deps = Seq(
    testWith(validation, withTests = true),
    testWith(scalatestsuite),
    testWith(`article-api`, withTests = true),
    testWith(`draft-api`, withTests = true),
    testWith(`learningpath-api`, withTests = true),
    testWith(`search-api`, withTests = true)
  )
)
