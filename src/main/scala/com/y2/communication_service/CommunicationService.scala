package com.y2.communication_service

import akka.actor.{Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

/**
  * Service that handles communication in the y2 cluster.
  */
class CommunicationService extends Actor with ActorLogging {
  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = {
    log.info("Worker started.")
    log.info("Trying to connect to the cluster.")

    // Akka Management hosts the HTTP routes used by bootstrap
    AkkaManagement(context.system).start()

    // Starting the bootstrap process needs to be done explicitly
    ClusterBootstrap(context.system).start()

    // Subscribe to MemberUp messages to perform setup actions when the node joins the cluster
    cluster.subscribe(self, classOf[MemberUp])
  }

  /**
    * Unsubscribe from the cluster when stopping the actor.
    */
  override def postStop(): Unit = cluster.unsubscribe(self)

  /**
    * Handle received messages.
    * @return a function that handles the received messages.
    */
  @Override
  def receive = {
    case MemberUp(m) => log.info(m + " is up.")
  }

//  def register(member: Member): Unit =
//    if (member.hasRole("frontend"))
//      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
//        BackendRegistration
//}

}