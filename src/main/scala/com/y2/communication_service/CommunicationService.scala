package com.y2.communication_service

import akka.actor.{Actor, ActorLogging, ActorRef, RootActorPath, Terminated}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, ClusterEvent}
import com.y2.client_service.MessageSequence
import com.y2.messages.ClientCommunicationMessage._

/**
  * Service that handles communication in the y2 cluster.
  */
class CommunicationService extends Actor with ActorLogging with MessageSequence {
  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * The client from which to get instructions to execute.
    * Null, when no client ever responded.
    */
  private var client: ActorRef = _

  /**
    * Received data
    */
  private var data = scala.collection.mutable.Queue[(Array[Byte], String)]()

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = {
    // Subscribe to MemberUp messages to perform setup actions when the node joins the cluster
    cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[MemberUp])
    // Subscribe to MemberRemoved to learn if the connected client went down
    cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[MemberRemoved])
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
  def receive: PartialFunction[Any, Unit] = receiveChunks orElse {
    case MemberUp(member) =>
      log.info(member + " is up.")
      if (client == null && member.hasRole("client")) {
        log.info("A client is up. Sending a client request.")
        context.actorSelection(RootActorPath(member.address) / "user" / "client") ! ClientRequest
    }

    case Terminated(actor) =>
      if (actor.equals(client)) {
        log.info("Client died.")
        client = null
      }

    case ClientAnswer =>
      // Watch the client that answered to monitor whether it dies at some point.
      context.watch(sender())
      client = sender()
      log.info("A client answered. Requesting training data.")
      client ! TrainingDataRequest()

    case trainingData: TrainingDataAnswer =>
      data.enqueue((trainingData.data, trainingData.reference))
      log.info("Received data with reference value " + trainingData.reference)
  }



}