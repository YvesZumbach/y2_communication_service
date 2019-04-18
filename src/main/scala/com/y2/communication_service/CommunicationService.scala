package com.y2.communication_service

import akka.actor.{Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster

/**
  * Service that handles communication in the y2 cluster.
  */
class CommunicationService extends Actor with ActorLogging {
  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * Subscribe to the cluster when when starting the actor.
    */
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  /**
    * Unsubscribe from the cluster when stopping the actor.
    */
  override def postStop(): Unit = cluster.unsubscribe(self)

  /**
    * Handle received messages.
    * @return a function that handles the received messages.
    */
  def receive = {
    case Communi
  }
}