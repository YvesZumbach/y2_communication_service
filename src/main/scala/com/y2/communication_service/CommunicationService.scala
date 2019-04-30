package com.y2.communication_service

import java.nio.{ByteBuffer, ByteOrder}
import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, RootActorPath, Terminated}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, ClusterEvent}
import com.y2.messages.Message._


/**
  * Service that handles communication in the y2 cluster.
  */
class CommunicationService extends Actor with ActorLogging {
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
    * The index of this node in the cluster.
    */
  private var nodeIndex: Int = _

  /**
    * Communication with the worker service.
    */
  private val workerCommunication = new WorkerCommunication(this.self, context)

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
  def receive: PartialFunction[Any, Unit] = {
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
      log.info("A client answered.")

    case message: WorkerToCommunicationMessage =>
      message.messageType match {
        case CommunicationService.DELTA_MESSAGE =>
          context.system.actorSelection("/user/node") ! Delta(message.message)
        case CommunicationService.RUNTIME_MESSAGE =>
          // Extract time spent on each tasks
          val buffer = ByteBuffer.wrap(message.message).asReadOnlyBuffer()
          val sampleCount = buffer.getInt(0)
          val decompressionMilli = buffer.getInt(4)
          val trainingMilli = buffer.getInt(8)
          val compressionMilli = buffer.getInt(12)
          // Send Runtime message to the client that will do some stats
          client ! Runtime(sampleCount, decompressionMilli, trainingMilli, compressionMilli)
        case CommunicationService.FINISHED_MESSAGE =>
          client ! Finished(Instant.now)
      }

    case NodeIndex(index, total) =>
      val message = ByteBuffer.allocate(8)
      message.order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(index).put(total)
      workerCommunication.send(CommunicationService.NODE_INDEX_MESSAGE, message.array())

    case Delta(deltas) => workerCommunication.send(CommunicationService.DELTA_MESSAGE, deltas)
  }
}

object CommunicationService {
  val NODE_INDEX_MESSAGE = 0
  val DELTA_MESSAGE = 1
  val RUNTIME_MESSAGE = 2
  val FINISHED_MESSAGE = 3
}