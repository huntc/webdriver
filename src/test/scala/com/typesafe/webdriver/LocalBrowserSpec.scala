package com.typesafe.webdriver

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import akka.testkit._
import akka.actor.ActorRef
import java.io.File
import scala.concurrent.{Promise, Future}
import spray.json.{JsNull, JsValue, JsArray}

// Note that this test will only run on Unix style environments where the "rm" command is available.
@RunWith(classOf[JUnitRunner])
class LocalBrowserSpec extends Specification {

  object TestWebDriverCommands extends WebDriverCommands {
    def createSession(): Future[String] = Promise.successful("123").future

    def destroySession(sessionId: String) {}

    def executeJs(sessionId: String, script: String, args: JsArray): Future[JsValue] = Future.successful(JsNull)
  }

  "The local browser" should {
    "be started when requested, remove a file and then shutdown" in new TestActorSystem {
      val f = File.createTempFile("LocalBrowserSpec", "")
      f.deleteOnExit()

      val localBrowser = TestFSMRef(new LocalBrowser(Session.props(TestWebDriverCommands), Some(Seq("rm", f.getCanonicalPath))))

      val probe = TestProbe()
      probe watch localBrowser

      localBrowser.stateName must_== LocalBrowser.Uninitialized

      localBrowser ! LocalBrowser.Startup

      localBrowser.stateName must_== LocalBrowser.Started

      localBrowser.stop()

      probe.expectTerminated(localBrowser)

      f.exists() must beFalse
    }

    "permit a session to be created" in new TestActorSystem {
      val localBrowser = TestFSMRef(new LocalBrowser(Session.props(TestWebDriverCommands), None))

      localBrowser ! LocalBrowser.Startup

      localBrowser ! LocalBrowser.CreateSession
      expectMsgClass(classOf[ActorRef])
    }
  }

}
