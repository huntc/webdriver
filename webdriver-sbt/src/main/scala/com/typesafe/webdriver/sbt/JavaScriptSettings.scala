package com.typesafe.webdriver.sbt

import sbt._
import sbt.Keys._

/**
 * Adds JavaScript settings to SBT
 */
object JavaScriptSettings extends Plugin {

  val JavaScript = config("js")
  val JavaScriptTest = config("js-test")
  val jsSource = SettingKey[File]("js-source", "The main source directory for JavaScript.")

  private def filterSources(sources: Seq[File], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] = {
    val filter = includeFilter -- excludeFilter
    sources.filter(filter.accept)
  }

  private def locateSources(sourceDirectory: File, includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectory ** (includeFilter -- excludeFilter)).get

  override def projectSettings = Seq(
    includeFilter in JavaScript := GlobFilter("*.js"),
    includeFilter in JavaScriptTest := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),

    // jsSource is just a directory that allows you to layout your project nicely, anything in it gets added to the
    // resources task.
    jsSource in Compile := (sourceDirectory in Compile).value / "js",
    jsSource in Test := (sourceDirectory in Test).value / "js",
    unmanagedResources in Compile <++= (jsSource in Compile, includeFilter in (Compile, unmanagedResources), excludeFilter in (Compile, unmanagedResources)) map locateSources,
    unmanagedResources in Test <++= (jsSource in Test, includeFilter in (Test, unmanagedResources), excludeFilter in (Test, unmanagedResources)) map locateSources,

    // The actual javascript sources come from whatever is in resources
    unmanagedSources in JavaScript <<= (unmanagedResources in Compile, includeFilter in JavaScript, excludeFilter in JavaScript) map filterSources,
    managedSources in JavaScript <<= (managedResources in Compile, includeFilter in JavaScript, excludeFilter in JavaScript) map filterSources,
    unmanagedSources in JavaScriptTest <<= (unmanagedResources in Test, includeFilter in JavaScriptTest, excludeFilter in JavaScriptTest) map filterSources,
    managedSources in JavaScriptTest <<= (managedResources in Test, includeFilter in JavaScriptTest, excludeFilter in JavaScriptTest) map filterSources,
    sources in JavaScript <<= (unmanagedSources in JavaScript, managedSources in JavaScript) map(_ ++ _),
    sources in JavaScriptTest <<= (unmanagedSources in JavaScriptTest, managedSources in JavaScriptTest) map(_ ++ _)
  )
}
