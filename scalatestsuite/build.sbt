val scala213 = "2.13.3"
val Scalaversion = scala213
val MockitoVersion = "1.14.8"
val Json4SVersion = "3.6.7"
val FlywayVersion = "7.1.1"
val PostgresVersion = "42.2.14"
val HikariConnectionPoolVersion = "3.4.5"
val CatsEffectVersion = "2.1.1"
val TestContainersVersion = "1.15.1"

val ScalaTestVersion = "3.2.1"

lazy val supportedScalaVersions = List(
  scala213
)

lazy val commonSettings = Seq(
  organization := "ndla",
  scalaVersion := Scalaversion,
  crossScalaVersions := supportedScalaVersions
)

lazy val scalatestsuite = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "scalatestsuite",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.44",
      "org.scalatest" %% "scalatest" % ScalaTestVersion,
      "org.mockito" %% "mockito-scala" % MockitoVersion,
      "org.mockito" %% "mockito-scala-scalatest" % MockitoVersion,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "org.testcontainers" % "elasticsearch" % TestContainersVersion,
      "org.testcontainers" % "testcontainers" % TestContainersVersion,
      "org.testcontainers" % "postgresql" % TestContainersVersion,
      "joda-time" % "joda-time" % "2.10"
    )
  )

val checkfmt = taskKey[Boolean]("Check for code style errors")
checkfmt := {
  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
  val noErrorsInTestFiles = (Test / scalafmtCheck).value
  val noErrorsInSbtConfigFiles = (Compile / scalafmtSbtCheck).value

  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInSbtConfigFiles
}

Test / test := ((Test / test).dependsOn(Test / checkfmt)).value

val fmt = taskKey[Unit]("Automatically apply code style fixes")
fmt := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

publishTo := {
  val nexus = sys.props.getOrElse("nexus.host", "")
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/ndla-snapshots")
  else
    Some("releases" at nexus + "content/repositories/ndla-releases")
}

resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq
