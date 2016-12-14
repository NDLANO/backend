val Scalaversion = "2.11.8"
val ScalaTestVersion = "2.2.4"

lazy val commonSettings = Seq(
  organization := "ndla",
  scalaVersion := Scalaversion
)

lazy val mapping = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "mapping",
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    scalacOptions := Seq("-target:jvm-1.7"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test"
    )
  )

publishTo := {
  val nexus = sys.props.getOrElse("nexus.host", "")
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/ndla-snapshots")
  else
    Some("releases"  at nexus + "content/repositories/ndla-releases")
}
