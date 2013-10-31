package com.typesafe.webdriver

import akka.actor.ActorSystem
import spray.json._
import scala.concurrent.Future
import com.typesafe.webdriver.WebDriverCommands.{Errors, WebDriverError, WebDriverErrorDetails}

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
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol
  import spray.httpx.PipelineException

  private case class CommandResponse(sessionId: String, status: Int, value: JsValue)

  private object CommandProtocol extends DefaultJsonProtocol {
    implicit val commandResponse = jsonFormat3(CommandResponse)

    implicit object stackTraceElementFormat extends JsonFormat[StackTraceElement] {
      def write(ste: StackTraceElement) = JsObject(
        "fileName" -> JsString(ste.getFileName),
        "className" -> JsNumber(ste.getClassName),
        "methodName" -> JsNumber(ste.getMethodName),
        "lineNumber" -> JsNumber(ste.getLineNumber)
      )

      def read(value: JsValue) = {
        value.asJsObject.getFields("fileName", "className", "methodName", "lineNumber") match {
          case Seq(JsString(fileName), JsString(className), JsString(methodName), JsNumber(lineNumber)) =>
            new StackTraceElement(className, methodName, fileName, lineNumber.toInt)
          case _ => throw new DeserializationException("Color expected")
        }
      }
    }

    implicit val webDriverErrorDetailsFormat = jsonFormat(WebDriverErrorDetails, "message", "screen", "class",
      "stackTrace")
  }

  def unmarshalIgnoreStatus[T: Unmarshaller]: HttpResponse => T = {
    response =>
      response.entity.as[T] match {
        case Right(value) ⇒ value
        case Left(error) ⇒ throw new PipelineException(error.toString)
      }
  }

  import CommandProtocol._

  private val pipeline: HttpRequest => Future[CommandResponse] = (
    addHeaders(
      Host(host, port),
      Accept(Seq(MediaTypes.`application/json`, MediaTypes.`image/png`))
    )
      ~> sendReceive
      ~> unmarshalIgnoreStatus[CommandResponse]
    )

  override def createSession(): Future[String] = {
    pipeline(Post("/session", """{"desiredCapabilities": {}}""")).withFilter(_.status == 0).map(_.sessionId)
  }

  override def destroySession(sessionId: String) {
    pipeline(Delete(s"/session/$sessionId/window"))
  }

  override def executeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]] = {
    pipeline(Post(s"/session/$sessionId/execute", s"""{"script":${JsString(s"$script;return result;")},"args":$args}"""))
      .map {
      response =>
        if (response.status == Errors.Success) {
          Right(response.value)
        } else {
          Left(WebDriverError(response.status, response.value.convertTo[WebDriverErrorDetails]))
        }
    }
  }
}