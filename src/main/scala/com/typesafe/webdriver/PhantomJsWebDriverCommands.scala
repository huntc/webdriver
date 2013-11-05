package com.typesafe.webdriver

import spray.json.{JsString, JsValue, JsArray}
import scala.concurrent.Future
import com.typesafe.webdriver.WebDriverCommands.WebDriverError
import akka.actor.ActorSystem
import spray.client.pipelining._

/**
 * Specialisations for PhantomJs.
 */
class PhantomJsWebDriverCommands(host: String, port: Int)(implicit system: ActorSystem)
  extends HttpWebDriverCommands(host, port) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def executeNativeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]] = {
    pipeline(Post(s"/session/$sessionId/phantom/execute", s"""{"script":${JsString(s"$script;return result;")},"args":$args}"""))
      .map(toEitherErrorOrValue)
  }
}
