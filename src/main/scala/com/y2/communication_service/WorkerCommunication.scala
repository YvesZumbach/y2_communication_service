package com.y2.communication_service

import java.net.InetSocketAddress
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.channels.{AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.{ActorContext, ActorRef}
import akka.event.{Logging, LoggingAdapter}
import com.y2.messages.Message.WorkerToCommunicationMessage

class WorkerCommunication {
  private val handshakeLength = 8

  private final var receiver: ActorRef = _
  private final var context: ActorContext = _

  private val server: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8888))
  private var socket: AsynchronousSocketChannel = _

  private val sendQueue: ConcurrentLinkedQueue[(Int, Array[Byte])] = new ConcurrentLinkedQueue[(Int, Array[Byte])]()
  private var buffer: ByteBuffer = ByteBuffer.allocate(handshakeLength)

  private final var log: LoggingAdapter = _

  private var sendingThread: Thread = _

  private val acceptCompletionHandler: CompletionHandler[AsynchronousSocketChannel, Void] = new CompletionHandler[AsynchronousSocketChannel, Void] {
    override def completed(v: AsynchronousSocketChannel, a: Void): Unit = {
      log.info("Connection established with worker at address " + v.getRemoteAddress)
      socket = v
      // Start read and writes on the socket
      start()
    }

    override def failed(throwable: Throwable, a: Void): Unit = {
      log.error("Failed to accept connection from worker: " + throwable)
      log.info("Listening again for incoming worker service connections...")
      server.accept(null, acceptCompletionHandler)
    }
  }

  private val readCompletionHandler: CompletionHandler[Integer, Void] = new CompletionHandler[Integer, Void] {
    override def completed(v: Integer, a: Void): Unit = {
      // -1 means the connection was closed
      if (v == -1) {
        log.error("Communication with the worker service was interrupted.")
        log.info("Listening again for incoming worker service connections...")
        sendingThread.interrupt()
        // Start listening again for incoming connections
        server.accept(null, acceptCompletionHandler)
        return
      }
      if (v == 0) {
        // A message of length zero is sent to initialize the communication. It should not be processed
        restartRead()
        return
      }
      if (v != handshakeLength) {
        log.error("Wrong number of byte received for the size message. Expected " + handshakeLength + ", received " + v)
        restartRead()
        return
      }
      // Allocate new buffer for next read operation of correct size
      val messageType = buffer.getInt(0)
      val declaredSize = buffer.getInt(4)
      // Start synchronous read
      buffer = ByteBuffer.allocate(declaredSize)
      val receivedSize = socket.read(buffer).get()
      if (declaredSize != receivedSize) {
        log.error(s"Received $receivedSize byte, $declaredSize were expected. Dropping this message.")
        restartRead()
        return
      }
      log.info("Received a message of type "
        + messageType
        + " of length " + declaredSize
        + " bytes from the worker service")
      // Copy received bytes into an array
      val message = buffer.array()
      restartRead()
      // Execute read callback
      receiver ! WorkerToCommunicationMessage(messageType, message)
    }

    private def restartRead(): Unit = {
      // Create new buffer for next size message
      buffer = createEmptyHandshakeBuffer()
      // Start asynchronous read again
      socket.read(buffer, null, readCompletionHandler)
    }

    override def failed(throwable: Throwable, a: Void): Unit = {
      log.error("Failed to receive a message from the worker: " + throwable)

      log.info("Communication with the worker service was reset. Listening again for incoming worker service connections...")
      sendingThread.interrupt()
      // Start listening again for incoming connections
      server.accept(null, acceptCompletionHandler)
    }
  }

  def this(receiver: ActorRef, context: ActorContext) = {
    this()

    this.receiver = receiver
    this.context = context
    this.log = Logging(context.system, "worker communication")

    // Server accepts incoming connections
    log.info("Listening for incoming worker service connections...")
    server.accept(null, acceptCompletionHandler)
  }

  private def start(): Unit = {
    // Start reading
    socket.read(buffer, null, readCompletionHandler)
    log.info("Listening for message from worker at address " + socket.getRemoteAddress)

    // Start sending
    sendingThread = new Thread {
      override def run() {
        try {
          while (true) {
            // Wait until a message to send is received
            while (sendQueue.isEmpty) sendQueue.synchronized { sendQueue.wait() }
            // Retrieve the message to send
            val (messageType, message) = sendQueue.poll()
            // Send the message size
            val handshakeBuffer = createHandshakeBuffer(messageType, message.length)
            socket.write(handshakeBuffer).get()
            // Send the message synchronously
            socket.write(ByteBuffer.wrap(message)).get()
            log.info("Queued message was sent to the worker service.")
          }
        } catch {
          case e: InterruptedException => // Do nothing
        }
      }
    }
    sendingThread.start()
  }

  def send(messageType: Int, message: Array[Byte]) : Unit = {
    sendQueue.add((messageType, message))
    log.info("A message was added into the queue of message to send to the worker service.")
    sendQueue.synchronized { sendQueue.notifyAll() }
  }

  private def createEmptyHandshakeBuffer(): ByteBuffer = {
    val sizeMessageBuffer = ByteBuffer.allocate(handshakeLength)
    sizeMessageBuffer.order(ByteOrder.BIG_ENDIAN)
    sizeMessageBuffer
  }

  private def createHandshakeBuffer(messageType: Int, size: Int): ByteBuffer = {
    val sizeMessageBuffer = createEmptyHandshakeBuffer()
    sizeMessageBuffer.asIntBuffer().put(messageType).put(size)
    sizeMessageBuffer
  }
}
