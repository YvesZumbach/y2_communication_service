package com.y2.communication_service

import org.zeromq.{SocketType, ZContext, ZMsg}
import java.util.concurrent.ConcurrentLinkedQueue

import org.zeromq.ZMQ.Poller

class WorkerCommunication {
  /**
    * The ZeroMQ context.
    */
  private val zcontext: ZContext = new ZContext()

  /**
    * Publisher socket
    */
  private val workerServiceConnectionPub = zcontext.createSocket(SocketType.PUB)
  workerServiceConnectionPub.bind("ipc://*:5563")

  /**
    * Subscriber socket
    */
  private val workerServiceConnectionSub = zcontext.createSocket(SocketType.SUB)
  workerServiceConnectionSub.bind("ipc://*:5564")

  private val sendQueue : ConcurrentLinkedQueue[Array[Byte]] = new ConcurrentLinkedQueue[Array[Byte]]()

  def WorkerCommunication() : Unit = {
    val sendingThread = new Thread {
      override def run() {
        while (true) {
          while (sendQueue.isEmpty) sendQueue.wait()
          val msg = sendQueue.poll()
          workerServiceConnectionPub.send(msg)
        }
      }
    }
    sendingThread.start()

    val receiveThread = new Thread {
      override def run(): Unit = {
        val poller : Poller = zcontext.createPoller(1)
        poller.register(workerServiceConnectionSub)
        while (true) {
          poller.poll(-1)
          if (poller.pollin(0)) {
            val incomingMsg : ZMsg = ZMsg.recvMsg(workerServiceConnectionSub)
            var incomingBytes : Array[Byte] = _
            incomingMsg.toArray(incomingBytes)

          }
        }
      }
    }
  }

  def send(msg: Array[Byte]) : Unit = {
    sendQueue.add(msg)
    sendQueue.notifyAll()
  }


}
