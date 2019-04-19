package com.y2.runtype

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.y2.communication_service.CommunicationService
import com.y2.config.Y2Config

/**
  * Run a node.
  * @param config the configuration to apply to the node.
  */
class Y2Node(val config: Y2Config) {
}

object Y2Node {
  var index = new AtomicInteger()
}