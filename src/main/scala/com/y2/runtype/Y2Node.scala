package com.y2.runtype

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.scalalogging.Logger
import com.y2.communication_service.CommunicationService
import com.y2.config.Config


class NodeRun(val nodeConfig: Config)(implicit system: ActorSystem) {

  val log = Logger(classOf[NodeRun])

  log.info("Starting node...")

  val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  log.info("Communication service started.")

  // Start worker service here
  log.info("Worker service started.")

  log.info("Node completed startup.")
}
