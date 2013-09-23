organization := "com.typesafe"

name := "webdriver"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1",
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "junit" % "junit" % "4.11" % "test"
)
