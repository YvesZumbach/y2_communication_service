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
  final case class AudioTranscript(text: String, uuid: String)

  /**
    * Contains the actual audio data.
    * @param data audio data.
    */
  final case class AudioData(data: Array[Byte], uuid: String)

  /**
    * Sent by node to clients to retrieve their ActorRef.
    */
  final case class ClientRequest()

  /**
    * Sent by client to nodes after a ClientRequest message to prove they are alive and sent their ActorRef.
    */
  final case class ClientAnswer()

}