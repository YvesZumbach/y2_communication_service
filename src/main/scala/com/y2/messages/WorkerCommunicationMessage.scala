package com.y2.messages

class WorkerCommunicationMessage { }

case class FromWorker(message: Array[Byte]) extends WorkerCommunicationMessage

case class ToWorker(message: Array[Byte]) extends WorkerCommunicationMessage
