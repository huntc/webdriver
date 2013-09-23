package com.typesafe.webdriver

import akka.actor.{FSM, Props, Actor}
import java.io.File
import com.typesafe.webdriver.Session._
import scala.Some
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * Browsers maintain sessions for the purposes of our interactions with them. Sessions can be requested to do things
 * such as create a document or execute some JavaScript.
 * @param wd the commands to use for communicating with a web driver host.
 * @param sessionConnectTimeout the timeout value for waiting on session establishment.
 */
class Session(wd: WebDriverCommands, sessionConnectTimeout: FiniteDuration)
  extends Actor
  with FSM[State, Option[Int]] {

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(Connect, None) =>
      wd.createSession().onComplete {
        case Success(sessionId) => self ! SessionCreated(sessionId)
        case Failure(t) => {
          log.error("Stopping due to not being able to establish a session - exception thrown - {}.", t)
          stop()
        }
      }
      goto(Connecting) using None
  }

  when(Connecting, stateTimeout = sessionConnectTimeout) {
    case Event(SessionCreated(sessionId), None) => goto(Connected) using Some(sessionId)
    case Event(StateTimeout, None) => {
      log.error("Stopping due to not being able to establish a session - timed out.")
      stop()
    }
    case Event(e: ExecuteJs, None) => {
      self ! e
      stay()
    }
  }

  when(Connected) {
    case Event(e: ExecuteJs, someSessionId@Some(_)) => {
      someSessionId.foreach(wd.executeJs(_, e.jsFile))
      stay()
    }
  }

  onTermination {
    case StopEvent(_, _, someSessionId@Some(_)) => someSessionId.foreach(wd.destroySession)
  }

  initialize()
}

object Session {

  /**
   * Connect a session.
   */
  case object Connect

  /**
   * Execute a JavaScript file given its location on the local file system.
   * @param jsFile the location of the JS file.
   */
  case class ExecuteJs(jsFile: File)


  /**
   * A convenience for creating the actor.
   */
  def props(wd: WebDriverCommands, sessionConnectTimeout: FiniteDuration = 2.seconds): Props =
    Props(classOf[Session], wd, sessionConnectTimeout)


  // Internal messages

  private[webdriver] case class SessionCreated(sessionId: Int)


  // Internal FSM states

  private[webdriver] trait State

  private[webdriver] case object Uninitialized extends State

  private[webdriver] case object Connecting extends State

  private[webdriver] case object Connected extends State

}
