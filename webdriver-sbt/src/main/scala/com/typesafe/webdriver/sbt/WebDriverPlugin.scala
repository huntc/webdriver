package com.typesafe.webdriver.sbt

import sbt._
import sbt.Keys._
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.gracefulStop
import com.typesafe.webdriver.{LocalBrowser, PhantomJs}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Declares the main parts of a WebDriver based plugin for sbt.
 */
abstract class WebDriverPlugin extends sbt.Plugin {

  object WebDriverKeys {
    val webBrowser = TaskKey[ActorRef]("js-web-browser", "An actor representing the webdriver browser.")
    val parallelism = SettingKey[Int]("js-parallelism", "The number of parallel tasks for the webdriver host. Defaults to the # of available processors + 1 to keep things busy.")
    val reporter = TaskKey[LoggerReporter]("js-reporter", "The reporter to use for conveying processing results.")
  }

  import WebDriverKeys._

  implicit val webDriverSystem = withActorClassloader(ActorSystem("webdriver-system"))
  implicit val webDriverTimeout = Timeout(5.seconds)

  private val browserAttrKey = AttributeKey[ActorRef]("web-browser")
  private val browserOwnerAttrKey = AttributeKey[WebDriverPlugin]("web-browser-owner")

  private def load(state: State): State = {
    state.get(browserOwnerAttrKey) match {
      case None => {
        val browser = webDriverSystem.actorOf(PhantomJs.props(), "localBrowser")
        browser ! LocalBrowser.Startup
        val newState = state.put(browserAttrKey, browser).put(browserOwnerAttrKey, this)
        newState.addExitHook(unload(newState))
      }
      case _ => state
    }
  }

  private def unload(state: State): State = {
    state.get(browserOwnerAttrKey) match {
      case Some(browserOwner: WebDriverPlugin) if browserOwner eq this =>
        state.get(browserAttrKey).foreach {
          browser =>
            try {
              val stopped: Future[Boolean] = gracefulStop(browser, 250.millis)
              Await.result(stopped, 500.millis)
            } catch {
              case _: Throwable =>
            }
        }
        state.remove(browserAttrKey).remove(browserOwnerAttrKey)
      case _ => state
    }
  }

  override val globalSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value andThen (load),
    onUnload in Global := (onUnload in Global).value andThen (unload),
    webBrowser <<= (state) map (_.get(browserAttrKey).get),
    parallelism := java.lang.Runtime.getRuntime.availableProcessors() + 1,
    reporter := new LoggerReporter(5, streams.value.log)
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
