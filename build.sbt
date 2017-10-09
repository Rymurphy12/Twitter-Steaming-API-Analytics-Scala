import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.rymurphy12",
      scalaVersion := "2.12.1",
      version      := "1.1.3-SNAPSHOT"
    )),
    name := "analytics",

    resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                      Resolver.typesafeIvyRepo("releases")

    ),

    libraryDependencies ++= Seq(scalaTest % Test,
    		  "com.danielasfregola" %% "twitter4s" % "5.1",
                  "com.typesafe" % "config" % "1.3.1",
                  "com.vdurmont" % "emoji-java" % "3.3.0"
    )
  )
