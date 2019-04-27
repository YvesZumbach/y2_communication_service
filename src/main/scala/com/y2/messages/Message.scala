package com.y2.messages

/**
  * Represent all messages exchanged between the client and the communication service.
  */
object Message {
  /**
    * Sent by node to clients to retrieve their ActorRef.
    */
  final case class ClientRequest()

  /**
    * Sent by client to nodes after a ClientRequest message to prove they are alive and sent their ActorRef.
    */
  final case class ClientAnswer()

  /**
    * Node index messages contain the information needed by the nodes to know the range of training example they must
    * process.
    */
  final case class NodeIndex(index: Int, total: Int)

  final case class Delta(deltas: Iterable[Int])

  /**
    * Wraps all messages received from th worker of a given node.
    * @param messageType An integer describing the type of message received.
    * @param message The actual content of the message.
    */
  final case class WorkerToCommunicationMessage(messageType: Int, message: Array[Byte])
}