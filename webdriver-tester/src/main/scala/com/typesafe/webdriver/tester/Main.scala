package com.typesafe.webdriver.tester

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.pattern.gracefulStop

import com.typesafe.webdriver.{Session, PhantomJs, LocalBrowser}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._
import scala.concurrent.{Future, Await}

object Main {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("webdriver-system")
    implicit val timeout = Timeout(5.seconds)

    system.scheduler.scheduleOnce(7.seconds) {
      system.shutdown()
      System.exit(1)
    }

    val browser = system.actorOf(PhantomJs.props(), "localBrowser")
    browser ! LocalBrowser.Startup
    for (
      session <- (browser ? LocalBrowser.CreateSession).mapTo[ActorRef];
      result <- (session ? Session.ExecuteJs("return arguments[0]", JsArray(JsNumber(999)))).mapTo[JsNumber]
    ) yield {
      println(result)

      try {
        val stopped: Future[Boolean] = gracefulStop(browser, 1.second)
        Await.result(stopped, 2.seconds)
        System.exit(0)
      } catch {
        case _: Throwable =>
      }

    }

  }
}
