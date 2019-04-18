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

  import akka.actor.ActorSystem

  val system: ActorSystem = ActorSystem.create("Appka")

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = {
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
  def receive = { }

  def receive = {
    case TransformationJob(text) => sender() ! TransformationResult(text.toUpperCase)
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up).foreach(register)
    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        BackendRegistration
}

}