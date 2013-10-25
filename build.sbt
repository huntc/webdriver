organization := "com.typesafe"

name := "webdriver-root"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "spray nightlies repo" at "http://nightlies.spray.io",
  "spray repo" at "http://repo.spray.io/"
  )


lazy val root = project.in( file(".") ).aggregate(webdriver, `webdriver-sbt`, `webdriver-tester`)

lazy val webdriver = project

lazy val `webdriver-sbt` = project.dependsOn(webdriver)

lazy val `webdriver-tester` = project.dependsOn(webdriver)
