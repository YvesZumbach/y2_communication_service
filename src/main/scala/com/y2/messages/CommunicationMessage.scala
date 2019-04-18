package com.y2.messages

import akka.cluster.ClusterMessage

/**
  * Represent all messages exchanged between communication services.
  */
class CommunicationMessage extends ClusterMessage { }

case class NeuralNetworkDeltas(deltas: Iterable[Int]) extends CommunicationMessage