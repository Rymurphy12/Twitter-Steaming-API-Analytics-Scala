import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.rymurphy12",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "SteamingApi",

    resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                      Resolver.typesafeIvyRepo("releases")

    ),

    libraryDependencies ++= Seq(scalaTest % Test,
    							"com.danielasfregola" %% "twitter4s" % "5.1",
                  "com.typesafe" % "config" % "1.3.1",
                  "com.lightbend" %% "emoji" % "1.1.1"
    )
  )
