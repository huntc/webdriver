package com.typesafe.webdriver

import scala.concurrent.Future
import spray.json.{JsString, JsArray, JsValue}

/**
 * Encapsulates all of the request/reply commands that can be sent via the WebDriver protocol. All commands perform
 * asynchronously and are non-blocking.
 */
abstract class WebDriverCommands {
  /**
   * Start a session
   * @return the future session id
   */
  def createSession(): Future[String]

  /**
   * Stop an established session. Performed on a best-effort basis.
   * @param sessionId the session to stop.
   */
  def destroySession(sessionId: String): Unit

  /**
   * Execute some JS code and return the status of execution
   * @param sessionId the session
   * @param script the script to execute
   * @param args a json array declaring the arguments to pass to the script
   * @return the return value of the script's execution as a json value
   */
  def executeJs(sessionId: String, script: String, args: JsArray): Future[JsValue]
}

import akka.actor.ActorSystem

/**
 * Communicates with a web driver host via http and json.
 * @param host the host of the webdriver
 * @param port the port of the webdriver
 */
class HttpWebDriverCommands(host: String, port: Int)(implicit system: ActorSystem) extends WebDriverCommands {

  import scala.concurrent.ExecutionContext.Implicits.global
  import spray.client.pipelining._
  import spray.http._
  import spray.http.HttpHeaders._
  import spray.httpx.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol

  private case class CommandResponse(sessionId: String, status: Int, value: JsValue)

  private object CommandProtocol extends DefaultJsonProtocol {
    implicit val commandResponse = jsonFormat3(CommandResponse)
  }

  import CommandProtocol._

  private val pipeline: HttpRequest => Future[CommandResponse] = (
    addHeaders(
      Host(host, port),
      Accept(Seq(MediaTypes.`application/json`, MediaTypes.`image/png`))
    )
      ~> sendReceive
      ~> unmarshal[CommandResponse]
    )

  def createSession(): Future[String] = {
    pipeline(Post("/session", """{"desiredCapabilities": {}}""")).withFilter(_.status == 0).map(_.sessionId)
  }

  def destroySession(sessionId: String) {
    pipeline(Delete(s"/session/$sessionId/window"))
  }

  def executeJs(sessionId: String, script: String, args: JsArray): Future[JsValue] = {
    pipeline(Post(s"/session/$sessionId/execute", s"""{"script":${JsString(script)},"args":$args}"""))
      .withFilter(_.status == 0)
      .map(_.value)
  }
}