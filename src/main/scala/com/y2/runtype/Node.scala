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
class Node(val config: Y2Config) {

  // Use special configuration in order to run several nodes on one computer
  if (config.local) {
    val index = Node.index.incrementAndGet()
    val config: Config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.hostname = "127.0.0.$index"
      akka.management.http.hostname = "127.0.0.$index"
      akka.remote.artery.canonical.port = 255$index
    """).withFallback(ConfigFactory.load())
    val system = ActorSystem("local-cluster", config)
    val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  } else {
    // Use default configuration
    val system = ActorSystem("local-cluster")
    val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  }
}

object Node {
  var index = new AtomicInteger()
}