package com.y2.messages

/**
  * Represent all messages exchanged between the client and the communication service.
  */
object ClientCommunicationMessage {

  /**
    * Sent by nodes to request data to process to the client.
    */
  final case class RequestData()

  /**
    * Contains the transcript of an audio file.
    * @param text the transcript.
    */
  final case class AudioTranscript(text: String)
}