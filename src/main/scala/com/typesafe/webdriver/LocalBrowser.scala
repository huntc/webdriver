package com.typesafe.webdriver

import akka.actor._
import scala.sys.process._
import com.typesafe.webdriver.LocalBrowser._
import scala.Some

/**
 * Provides an Actor on behalf of a browser. Browsers are represented as operating system processes and are
 * communicated with by using the http/json based WebDriver protocol.
 * @param sessionProps the properties required in order to produce a session actor.
 * @param maybeArgs a sequence of command line arguments used to launch the browser from the command line. If this is
 *                  set to None then the process is deemed to be controlled outside of this actor.
 */
class LocalBrowser(sessionProps: Props, maybeArgs: Option[Seq[String]]) extends Actor with FSM[State, Option[Process]] {

  startWith(Uninitialized, None)

  when(Uninitialized) {
    case Event(Startup, None) =>
      maybeArgs match {
        case Some(args) =>
          val p = Process(args).run(ProcessLogger(log.debug, log.error))
          goto(Started) using Some(p)
        case None => goto(Started) using None
      }
  }

  when(Started) {
    case Event(CreateSession, _) =>
      val session = context.actorOf(sessionProps)
      session ! Session.Connect
      sender ! session
      stay()
  }

  onTermination {
    case StopEvent(_, _, maybeProcess) =>
      maybeProcess.foreach(p => p.destroy())
  }

  initialize()
}

object LocalBrowser {

  /**
   * Start a browser. This is typically sent upon having obtained an actor ref to the browser.
   */
  case object Startup

  /**
   * Start a new session.
   */
  case object CreateSession


  // Internal FSM states

  private[webdriver] trait State

  private[webdriver] case object Uninitialized extends State

  private[webdriver] case object Started extends State

}

/**
 * Used to manage a local instance of PhantomJs. The default is to assume that phantomjs is on the path.
 */
object PhantomJs {
  def props(host: String = "127.0.0.1", port: Int = 8910)(implicit system: ActorSystem): Props = {
    val wd = new HttpWebDriverCommands(host, port)
    val args = Some(Seq("phantomjs", s"--webdriver=$host:$port"))
    Props(classOf[LocalBrowser], Session.props(wd), args)
  }
}