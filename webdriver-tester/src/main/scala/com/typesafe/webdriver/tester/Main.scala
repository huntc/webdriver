package com.typesafe.webdriver.tester

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask

import com.typesafe.webdriver.{Session, PhantomJs, LocalBrowser}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("webdriver-system")
    implicit val timeout = Timeout(5.seconds)

    val browser = system.actorOf(PhantomJs.props(), "localBrowser")
    browser ! LocalBrowser.Startup
    (browser ? LocalBrowser.CreateSession).mapTo[ActorRef].onSuccess {
      case s =>
        (s ? Session.ExecuteJs("return 1", "[]")).mapTo[String].onSuccess {
          case result =>
            println(result)
            system.shutdown()
        }
    }
  }
}
