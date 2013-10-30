package com.typesafe.webdriver

import com.gargoylesoftware.htmlunit.WebClient
import scala.collection.concurrent.TrieMap
import java.util.UUID
import com.gargoylesoftware.htmlunit.html.HtmlPage
import scala.concurrent.Future
import spray.json._
import com.typesafe.webdriver.WebDriverCommands.{WebDriverErrorDetails, Errors, WebDriverError}

/**
 * Runs webdriver command in the context of the JVM ala HtmlUnit.
 */
class HtmlUnitWebDriverCommands() extends WebDriverCommands {
  val sessions = TrieMap[String, HtmlPage]()

  import scala.concurrent.ExecutionContext.Implicits.global

  override def createSession(): Future[String] = {
    val webClient = new WebClient()
    val page: HtmlPage = webClient.getPage(WebClient.ABOUT_BLANK)
    val sessionId = UUID.randomUUID().toString
    sessions.put(sessionId, page)
    Future.successful(sessionId)
  }

  override def destroySession(sessionId: String): Unit = {
    sessions.remove(sessionId).foreach(_.getWebClient.closeAllWindows())
  }

  override def executeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]] = {
    sessions.get(sessionId).map({
      page =>
        Future {
          val scriptWithArgs = s"""|var args = JSON.parse('${args.toString().replaceAll("'", "\\'")}');
                                   |$script
                                   |""".stripMargin
          try {
            val scriptResult = page.executeJavaScript(scriptWithArgs)
            def toJsValue(v: Any): JsValue = {
              import scala.collection.JavaConverters._
              v match {
                case b: java.lang.Boolean => JsBoolean(b)
                case n: Number => JsNumber(n.doubleValue())
                case s: String => JsString(s)
                case n if n == null => JsNull
                case l: java.util.List[_] => JsArray(l.asScala.toList.map(toJsValue): _*)
                case o: java.util.Map[_, _] => JsObject(o.asScala.map(p => p._1.toString -> toJsValue(p._2)).toList)
                case x => JsString(x.toString)
              }
            }
            Right(toJsValue(scriptResult.getJavaScriptResult))
          } catch {
            case e: Exception =>
              Left(WebDriverError(
                Errors.JavaScriptError,
                WebDriverErrorDetails(e.getLocalizedMessage, stackTrace = Some(e.getStackTrace))))
          }
        }
    }).getOrElse(Future.successful(
      Left(WebDriverError(Errors.NoSuchDriver, WebDriverErrorDetails("Cannot locate sessionId")))))
  }
}
