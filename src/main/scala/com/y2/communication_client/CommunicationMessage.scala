package com.y2.communication_client

/**
  * Represent all messages exchanged between communication services.
  */
class CommunicationMessage { }

case class NeuralNetworkDeltas(deltas: Iterable[Int]) extends CommunicationMessage