package com.y2.runtype

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import com.y2.communication_service.CommunicationService
import com.y2.config.Y2Config

class Node(val nodeConfig: Y2Config, index: Int) {

  val config: Config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.hostname = "127.0.0.$index"
      akka.management.http.hostname = "127.0.0.$index"
    """).withFallback(ConfigFactory.load())
  val system = ActorSystem("local-cluster")

  val log = Logger(classOf[Node])

  log.info("Starting node...")

  val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  log.info("Communication service started.")

  // Start worker service here
  log.info("Worker service started.")

  log.info("Node completed startup.")
}
