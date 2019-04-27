package com.y2.client_service

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.{Cluster, ClusterEvent}
import com.y2.messages.ClientCommunicationMessage.{ClientAnswer, ClientRequest, NodeIndex}
import akka.cluster.ClusterEvent.MemberUp
import com.y2.config.Y2Config

class ClientService extends Actor with ActorLogging {

  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * The list of all registered nodes of the y2 cluster.
    */
  private var nodes: List[ActorRef] = Nil

  /**
    * Whether registration of nodes is still opened.
    */
  private var isRegistrationOpen = true

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
    case ClientRequest =>
      if (isRegistrationOpen) {
        log.info("Node " + sender() + " registered.")
        nodes = sender() :: nodes
        sender ! ClientAnswer
        if (nodes.length >= Y2Config.config.nodeCount) {
          log.info("All nodes registered to the client! Sending node index which will start the training.")
          isRegistrationOpen = false
          var count = 0
          nodes.foreach(node => {
            node ! NodeIndex(count, Y2Config.config.nodeCount)
            count = count + 1
          })
        }
      }
  }
}
