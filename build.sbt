val Scalaversion = "2.12.2"
val CrossScalaVersions = "2.11.8"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val AwsSdkversion = "1.11.93"
val ScalaLoggingVersion = "3.5.0"
val Json4sVersion = "3.5.0"

lazy val commonSettings = Seq(
  organization := "gdl",
  scalaVersion := Scalaversion,
  crossScalaVersions := Seq(CrossScalaVersions, Scalaversion)
)

lazy val language = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "language",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "org.json4s"   %% "json4s-native" % Json4sVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test")
  )

publishTo := {
  val nexus = sys.props.getOrElse("nexus.host", "")
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/gdl-snapshots")
  else
    Some("releases"  at nexus + "content/repositories/gdl-releases")
}
