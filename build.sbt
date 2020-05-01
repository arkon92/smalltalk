scalaVersion := "2.13.1"

ThisBuild / version := "0.1"

resolvers += Resolver.sonatypeRepo("snapshots")

val http4sVersion = "0.21.3"
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "2.1.3",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint:unused"
)
