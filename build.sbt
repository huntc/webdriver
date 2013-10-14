organization := "com.typesafe"

name := "webdriver"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "spray nightlies repo" at "http://nightlies.spray.io",
  "spray repo" at "http://repo.spray.io/"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "io.spray" % "spray-client" % "1.2-20130912",
  "io.spray" %% "spray-json" % "1.2.5",
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "junit" % "junit" % "4.11" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test"
)

lazy val root = project.in( file(".") )

lazy val `webdriver-sbt` = project.dependsOn(root)

lazy val `webdriver-tester` = project.dependsOn(root)
