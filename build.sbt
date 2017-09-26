import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.rymurphy12",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "SteamingApi",

    resolvers += Resolver.sonatypeRepo("releases"),

    libraryDependencies ++= Seq(scalaTest % Test,
    							"com.danielasfregola" %% "twitter4s" % "5.1",
                  "com.typesafe" % "config" % "1.3.1"
    )
  )
