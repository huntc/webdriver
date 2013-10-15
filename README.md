WebDriver
=========

WebDriver is a wire based protocol [drafted by the W3C](http://www.w3.org/TR/webdriver/)
in order to provide browsers with scripting capability.
WebDriver-enabled browsers include Chrome, Firefox, Safari, Internet Explorer and [PhantomJs](http://phantomjs.org/).
These browsers can be requested to execute JavaScript, load urls, enquire upon doms and more.

This project provides an alternate [WebDriver](http://www.seleniumhq.org/projects/webdriver/) implementation
based on Scala, Akka and Spray. The primary benefit of the approach taken here as opposed to many of the existing
WebDriver APIs is its [reactive nature](http://www.reactivemanifesto.org/) i.e. the API is non-blocking and event
driven, providing a resilient implementation by leveraging Akka in particular. One consequence of the API design is
that many WebDriver requests can be executed in parallel e.g. several JavaScript programs can be run
simultaneously.

Sample usage can be obtained by inspecting the webdriver-tester sub-project. There's a main class that
illustrates essential interactions. Here is a snippet of it:

    val browser = system.actorOf(PhantomJs.props(), "localBrowser")
    browser ! LocalBrowser.Startup
    for (
      session <- (browser ? LocalBrowser.CreateSession).mapTo[ActorRef];
      result <- (session ? Session.ExecuteJs("return arguments[0]", JsArray(JsNumber(999)))).mapTo[JsNumber]
    ) yield {
      println(result)
      ...

The above illustrates how a browser can be launched, a session established and then some arbitrary JavaScript
sent to it through the session.

An additional webdriver-sbt sub-project is provided that declares the a base for sbt plugins that use webdriver.
For example settings are declared that locate js main and test source files. This
sub-project has a separate release cycle to webdriver itself and could be spun off into its own repo at a later
point in time e.g. if/when Maven/Gradle support is required. The main point here is that the core webdriver
library is not related to sbt at all and should be usable from other build tools.

&copy; Typesafe Inc., 2013