package com.y2.client_service

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, ClusterEvent}
import com.y2.messages.ClientCommunicationMessage.{ClientAnswer, ClientRequest}

import akka.cluster.ClusterEvent.MemberUp

class ClientService extends Actor with ActorLogging {

  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[MemberUp])

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
    case MemberUp(m) => log.info("Member up!")
    case ClientRequest =>
      log.info("Received client request")
      sender ! ClientAnswer
  }
}
