package com.typesafe.webdriver

import scala.concurrent.Future
import java.io.File

/**
 * Encapsulates all of the request/reply commands that can be sent via the WebDriver protocol. All commands perform
 * asynchronously and are non-blocking.
 */
abstract class WebDriverCommands {
  def createSession(): Future[Int]

  def destroySession(sessionId: Int): Unit

  def executeJs(sessionId: Int, file: File)
}

/**
 * Communicates with a web driver host via http and json.
 * @param host the host of the webdriver
 * @param port the port of the webdriver
 */
class HttpWebDriverCommands(host: String, port: Int) extends WebDriverCommands{
  def createSession(): Future[Int] = ???

  def destroySession(sessionId: Int) {}

  def executeJs(sessionId: Int, file: File) {}
}