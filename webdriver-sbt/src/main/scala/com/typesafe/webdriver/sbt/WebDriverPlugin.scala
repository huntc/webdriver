package com.typesafe.webdriver.sbt

import sbt._
import sbt.Keys._
import akka.actor.{ActorSystem, ActorRef, PoisonPill}
import com.typesafe.webdriver.{LocalBrowser, PhantomJs}
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Declares the main parts of a WebDriver based plugin for sbt.
 */
abstract class WebDriverPlugin extends sbt.Plugin {

  object WebDriverKeys {
    val browser = TaskKey[ActorRef]("browser", "An actor representing the webdriver browser.")
    val JavaScript = config("js")
    val JavaScriptTest = config("js-test")
    val jsSource = SettingKey[File]("js-source", "The main source directory for JavaScript.")
    val parallelism = SettingKey[Int]("parallelism", "The number of parallel tasks for the webdriver host. Defaults to the # of available processors + 1 to keep things busy.")
    val reporter = TaskKey[LoggerReporter]("reporter", "The reporter to use for conveying processing results.")
  }

  import WebDriverKeys._

  def locateSources(sourceDirectory: File, includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectory * (includeFilter -- excludeFilter)).get

  def webDriverSettings = Seq(
    browser in JavaScript <<= (state) map (_.get(browserAttrKey).get),
    jsSource in JavaScript := (sourceDirectory in Compile).value / "js",
    jsSource in JavaScriptTest := (sourceDirectory in Test).value / "js",
    parallelism in JavaScript := java.lang.Runtime.getRuntime.availableProcessors() + 1,
    reporter in JavaScript := new LoggerReporter(5, streams.value.log),
    includeFilter in JavaScript := GlobFilter("*.js"),
    includeFilter in JavaScriptTest := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    sources in JavaScript <<= (jsSource in JavaScript, includeFilter in JavaScript, excludeFilter in JavaScript) map (locateSources),
    sources in JavaScriptTest <<= (jsSource in JavaScriptTest, includeFilter in JavaScriptTest, excludeFilter in JavaScriptTest) map (locateSources)
  )


  implicit val system = withActorClassloader(ActorSystem("webdriver-system"))
  implicit val timeout = Timeout(5.seconds)

  private val browserAttrKey = AttributeKey[ActorRef]("browser")
  private val browserOwnerAttrKey = AttributeKey[WebDriverPlugin]("browser-owner")

  // The basic assumption here is that onLoad and onUnload will only ever call one callback at a time.
  override val globalSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value andThen {
      state: State =>
        state.get(browserOwnerAttrKey) match {
          case None => {
            val browser = system.actorOf(PhantomJs.props(), "localBrowser")
            browser ! LocalBrowser.Startup
            state.put(browserAttrKey, browser).put(browserOwnerAttrKey, this)
            // FIXME: Need to install a shutdown hook so that the browser can be killed.
          }
          case _ => state
        }
    },

    onUnload in Global := (onUnload in Global).value andThen {
      state: State =>
        state.get(browserOwnerAttrKey) match {
          case Some(browserOwner: WebDriverPlugin) if browserOwner eq this =>
            val browser = state.get(browserAttrKey)
            browser.foreach(_ ! PoisonPill)
            state.remove(browserAttrKey)
          // FIXME: Uninstall the shutdown hook here.
          case _ => state
        }
    }
  )

  /*
   * Sometimes the class loader associated with the actor system is required e.g. when loading configuration in sbt.
   */
  private def withActorClassloader[A](f: => A): A = {
    val newLoader = ActorSystem.getClass.getClassLoader
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader

    thread.setContextClassLoader(newLoader)
    try {
      f
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }
}
