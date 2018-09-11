val Scalaversion = "2.12.6"
val CrossScalaVersions = "2.11.8"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val AwsSdkversion = "1.11.297"
val Json4sVersion = "3.5.3"
val JacksonVersion = "2.8.11.1"

lazy val commonSettings = Seq(
  organization := "ndla",
  scalaVersion := Scalaversion,
  crossScalaVersions := Seq(CrossScalaVersions, Scalaversion)
)

lazy val network = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "network",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in aws-java-sdk-s3 and json4s-jackson because of https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111
      "org.json4s"   %% "json4s-jackson" % Json4sVersion,
      "org.json4s"   %% "json4s-native" % Json4sVersion,
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided;test",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "com.pauldijou" %% "jwt-json4s-native" % "0.14.0")
  )

publishTo := {
  val nexus = sys.props.getOrElse("nexus.host", "")
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/ndla-snapshots")
  else
    Some("releases"  at nexus + "content/repositories/ndla-releases")
}

