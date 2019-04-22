package com.y2.communication_service

import java.net.{InetAddress, InetSocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.y2.messages.{FromWorker, ToWorker}

class WorkerCommunication(val receiver: ActorRef) extends Actor with ActorLogging{

  private val MaxFrameSizePropName = "akka.remote.netty.tcp.maximum-frame-size"
  private val maxFrameSize = context.system.settings.config.getBytes(MaxFrameSizePropName).toInt

  private val socket = AsynchronousSocketChannel.open()

  private val sendQueue: ConcurrentLinkedQueue[Array[Byte]] = new ConcurrentLinkedQueue[Array[Byte]]()

  private var buffer: ByteBuffer = ByteBuffer.allocate(maxFrameSize)

  private val sendingThread = new Thread {
    override def run() {
      while (true) {
        // Wait until a message to send is received
        while (sendQueue.isEmpty) sendQueue.wait()
        // Retrieve the message to send
        val msg = sendQueue.poll()
        // Send the message synchronously
        socket.write(ByteBuffer.wrap(msg)).get()
      }
    }
  }

  private val readCompletionHandler = new CompletionHandler[Integer, Void] {
    override def completed(v: Integer, a: Void): Unit = {
      // Extract received bytes
      val message = new Array[Byte](v)
      buffer.get(message)
      // Allocate new buffer for next read operation
      buffer = ByteBuffer.allocate(maxFrameSize)
      // Start asynchronous read again
      socket.read(buffer, null, readCompletionHandler)
      // Execute read callback
      if (receiver != null) receiver ! FromWorker(message)
    }

    override def failed(throwable: Throwable, a: Void): Unit = {
      log.error("Failed to receive a message: " + throwable)
    }
  }

  def WorkerCommunication(): Unit = {
    // Bind the socket; as soon as socket will be bound, reads and writes will start
    socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress, 1234), null, new CompletionHandler[Void, Void] {
      override def completed(v: Void, a: Void): Unit = start()

      override def failed(throwable: Throwable, a: Void): Unit = {
        log.error("Could not bind the address because an error occurred: " + throwable)
        // Crash the communication service
        sys.exit(1)
      }
    })
  }

  private def start(): Unit = {
    // Start reading
    socket.read(buffer, null, readCompletionHandler)

    // Start sending
    sendingThread.start()
  }

  private def send(msg: Array[Byte]) : Unit = {
    sendQueue.add(msg)
    sendQueue.notifyAll()
  }

  override def receive: Receive = {
    case ToWorker(message) => send(message)
  }
}
