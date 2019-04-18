package com.y2.node

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.scalalogging.Logger
import com.y2.Main
import com.y2.communication_service.CommunicationService
import picocli.CommandLine.Command

@Command(
  name = "node",
  descriptionHeading = "The work-horse of the y2 cluster (also known as 'a node') that will actually train your neural network."
)
class Node(implicit system: ActorSystem) {

  val log = Logger(classOf[Main])

  log.info("Node starting...")

  val communicationService: ActorRef = system.actorOf(Props[CommunicationService], "communication")
  log.info("Communication service started.")

  // Start worker service here
  log.info("Worker service started.")

  log.info("Node completed startup.")
}
