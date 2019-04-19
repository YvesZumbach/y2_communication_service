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
  if (config.local) {
    // Use special configuration in order to run several nodes on one computer
    val index = Y2Node.index.incrementAndGet()
    val config: Config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.hostname = "127.0.0.$index"
      akka.management.http.hostname = "127.0.0.$index"
      akka.remote.artery.canonical.port = 255$index
    """).withFallback(ConfigFactory.load())
    val system = ActorSystem("y2", config)
    val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  } else {
    // Use default configuration
    val system = ActorSystem("y2")
    val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  }
}

object Y2Node {
  var index = new AtomicInteger()
}