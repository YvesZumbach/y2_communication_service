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

  final case class Delta(deltas: Array[Byte])

  /**
    * Wraps all messages received from th worker of a given node.
    * @param messageType An integer describing the type of message received.
    * @param message The actual content of the message.
    */
  final case class WorkerToCommunicationMessage(messageType: Int, message: Array[Byte])

  /**
    * Sent after each epoch, contains the amount of time spent on each of the tasks the worker performs,
    * for measurements purposes
    * @param index The index of the node sending the message (see NodeIndex message)
    * @param decompressionMilli The time spent on decompressing the messages in milliseconds.
    * @param trainingMilli The time spent on training in milliseconds.
    * @param compressionMilli The time spent on compression the deltas in milliseconds.
    */
  final case class Runtime(index: Int, decompressionMilli: Int, trainingMilli: Int, compressionMilli: Int)

  /**
    * The message sent when a node finished training on all its samples
    * @param index The index of the node that completed (the index of the node is received from the client using the
    *              NodeIndex message)
    */
  final case class Finished(index: Int)
}