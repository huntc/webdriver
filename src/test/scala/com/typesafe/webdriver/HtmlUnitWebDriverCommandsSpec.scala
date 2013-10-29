package com.typesafe.webdriver

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import spray.json._
import scala.concurrent.{Await, Future}
import org.specs2.mutable.Specification
import org.specs2.time.NoDurationConversions
import org.specs2.matcher.MatchResult
import com.typesafe.webdriver.WebDriverCommands.WebDriverError

@RunWith(classOf[JUnitRunner])
class HtmlUnitWebDriverCommandsSpec extends Specification with NoDurationConversions {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  def withSession(block: (WebDriverCommands, String) => Future[Either[WebDriverError, JsValue]]): Future[Either[WebDriverError, JsValue]] = {
    val commands = new HtmlUnitWebDriverCommands()
    val maybeSession = commands.createSession().map {
      sessionId =>
        val result = block(commands, sessionId)
        commands.destroySession(sessionId)
        result
    }
    maybeSession.flatMap(x => x)
  }

  def testFor(v: JsValue): MatchResult[Any] = {
    val result = withSession {
      (commands, sessionId) =>
        commands.executeJs(sessionId, "args[0];", JsArray(v))
    }
    Await.result(result, Duration(1, SECONDS)) must_== Right(v)
  }

  "HtmlUnit" should {
    "execute js returning a boolean" in testFor(JsTrue)
    "execute js returning a number" in testFor(JsNumber(1))
    "execute js returning a string" in testFor(JsString("hi"))
    "execute js returning a null" in testFor(JsNull)
    "execute js returning an array" in testFor(JsArray(JsNumber(1)))
    "execute js returning an object" in testFor(JsObject("k" -> JsString("v")))
  }
}
