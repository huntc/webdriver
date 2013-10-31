package com.typesafe.webdriver

import scala.concurrent.Future
import spray.json._
import com.typesafe.webdriver.WebDriverCommands.WebDriverError

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
   * Execute some JS code and return the status of execution. Any args are passed to the script as args[]. The
   * script must also declare a result as a variable e.g. <pre>var result = "some result";</pre>. This result
   * variable will be looked for in the global scope and errors will occur if it is not found.
   * @param sessionId the session
   * @param script the script to execute
   * @param args a json array declaring the arguments to pass to the script
   * @return the return value of the script's execution as a json value
   */
  def executeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]]
}

object WebDriverCommands {

  object Errors {
    /** The command executed successfully. */
    val Success = 0

    /** A session is either terminated or not started */
    val NoSuchDriver = 6

    /** An element could not be located on the page using the given search parameters. */
    val NoSuchElement = 7

    /** A request to switch to a frame could not be satisfied because the frame could not be found. */
    val NoSuchFrame = 8

    /** The requested resource could not be found, or a request was received using an HTTP method that is not supported
      * by the mapped resource. */
    val UnknownCommand = 9

    /** An element command failed because the referenced element is no longer attached to the DOM. */
    val StaleElementReference = 10

    /** An element command could not be completed because the element is not visible on the page. */
    val ElementNotVisible = 11

    /** An element command could not be completed because the element is in an invalid state (e.g. attempting to click
      * a disabled element). */
    val InvalidElementState = 12

    /** An unknown server-side error occurred while processing the command. */
    val UnknownError = 13

    /** An attempt was made to select an element that cannot be selected. */
    val ElementIsNotSelectable = 15

    /** An error occurred while executing user supplied JavaScript. */
    val JavaScriptError = 17

    /** An error occurred while searching for an element by XPath. */
    val XPathLookupError = 19

    /** An operation did not complete before its timeout expired. */
    val Timeout = 21

    /** A request to switch to a different window could not be satisfied because the window could not be found. */
    val NoSuchWindow = 23

    /** An illegal attempt was made to set a cookie under a different domain than the current page. */
    val InvalidCookieDomain = 24

    /** A request to set a cookie's value could not be satisfied. */
    val UnableToSetCookie = 25

    /** A modal dialog was open, blocking this operation */
    val UnexpectedAlertOpen = 26

    /** An attempt was made to operate on a modal dialog when one was not open. */
    val NoAlertOpenError = 27

    /** A script did not complete before its timeout expired. */
    val ScriptTimeout = 28

    /** The coordinates provided to an interactions operation are invalid. */
    val InvalidElementCoordinates = 29

    /** IME was not available. */
    val IMENotAvailable = 30

    /** An IME engine could not be started. */
    val IMEEngineActivationFailed = 31

    /** Argument was an invalid selector (e.g. XPath/CSS). */
    val InvalidSelector = 32

    /** A new session could not be created. */
    val SessionNotCreatedException = 33

    /** Target provided for a move action is out of bounds. */
    val MoveTargetOutOfBounds = 34
  }

  /**
   * An error returned by WebDriver.
   *
   * @param status The error status code.
   * @param details The details of the error.
   */
  final case class WebDriverError(status: Int, details: WebDriverErrorDetails) {
    def toException = new WebDriverException(this)
  }

  /**
   * The details of an error returned by WebDriver
   *
   * @param message A descriptive message for the command failure.
   * @param screen If included, a screenshot of the current page as a base64 encoded string.
   * @param `class` If included, specifies the fully qualified class name for the exception that was thrown when the
   *                command failed.
   * @param stackTrace If included, specifies an array of JSON objects describing the stack trace for the exception
   *                   that was thrown when the command failed. The zeroeth element of the array represents the top of
   *                   the stack.
   */
  final case class WebDriverErrorDetails(message: String, screen: Option[String] = None, `class`: Option[String] = None,
                                         stackTrace: Option[Seq[StackTraceElement]] = None)

  /**
   * A WebDriver exception.
   *
   * The stack trace of this exception will be the stack trace of the JavaScript code on the remote system.
   *
   * @param error The WebDriverError triggered by this exception.
   */
  final class WebDriverException(val error: WebDriverError) extends RuntimeException(error.details.message) {
    error.details.stackTrace match {
      case Some(st) => setStackTrace(st.toArray)
      case None => setStackTrace(Array.empty)
    }
  }

}
