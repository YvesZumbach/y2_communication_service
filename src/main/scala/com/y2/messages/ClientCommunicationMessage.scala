package com.y2.messages

/**
  * Represent all messages exchanged between the client and the communication service.
  */
object ClientCommunicationMessage {
  final case class RequestData()
  final case class AudioTranscript(text: String)
}