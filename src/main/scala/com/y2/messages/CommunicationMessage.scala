package com.y2.messages

/**
  * Represent all messages exchanged between communication services.
  */
class CommunicationMessage { }

case class NeuralNetworkDeltas(deltas: Iterable[Int]) extends CommunicationMessage