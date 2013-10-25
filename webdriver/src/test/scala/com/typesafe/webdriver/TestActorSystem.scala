package com.typesafe.webdriver

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.specs2.mutable.After

/**
 * A utility that assists with Specs2 testing for Akka. Sets up a test actor system and then
 * brings it down after the test. Each test therefore gets its own actor system.
 */
abstract class TestActorSystem
  extends TestKit(ActorSystem())
  with After
  with ImplicitSender {

  def after = system.shutdown()
}