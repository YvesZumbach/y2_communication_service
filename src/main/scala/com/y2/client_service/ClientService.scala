package com.y2.client_service

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.actor.ActorSystem
import com.y2.messages.ClientCommunicationMessage.{RequestData, AudioTranscript}

import java.io.File
import scala.io.Source
import java.nio.file.{Files, Paths}

class ClientService extends MessageSequence with Actor with ActorLogging {

  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * The currently processed .txt file mapped
    */
  private var currentlyProcessed : Map[String, String] = Map()

  /**
    * Filenames to be processed
    */
  private var toBeProcessedFileNames : Set[String] = Set()

  /**
    * Currently processed file name
    */
  private var currentlyProcessedName : String = ""

  implicit val system: ActorSystem = ActorSystem.create("Appka")

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = {
    log.info("Client started.")
    log.info("Processing data.")

    toBeProcessedFileNames = getListOfFiles("").toSet

    // Akka Management hosts the HTTP routes used by bootstrap
    AkkaManagement(context.system).start()

    // Starting the bootstrap process needs to be done explicitly
    ClusterBootstrap(context.system).start()

    // Subscribe to RequestData messages to perform setup actions when the node joins the cluster
    cluster.subscribe(self, classOf[RequestData])
  }

  /**
    * Unsubscribe from the cluster when stopping the actor.
    */
  override def postStop(): Unit = cluster.unsubscribe(self)

  /**
    * Handle received messages.
    * @return a function that handles the received messages.
    */
  def receive = {
    case RequestData() => {
      sendMessageTo(sender())
    }
  }

  /**
    * Sends a message with the audio in bytes and with the transcript to the given Actor
    * @param to Actor to send a message to
    */
  private def sendMessageTo(to: ActorRef): Unit = {
    if (currentlyProcessed.isEmpty) {
      if (currentlyProcessedName.length != 0) {
        toBeProcessedFileNames -= currentlyProcessedName
      }
      currentlyProcessedName = toBeProcessedFileNames.head
      currentlyProcessed = readTextFile(currentlyProcessedName)
    }
    val nextAudioToSend = currentlyProcessed.head
    currentlyProcessed = currentlyProcessed.drop(1)
    val audioByteArray = Files.readAllBytes(Paths.get(nextAudioToSend._1))
    sendChunked(to, audioByteArray)
    to ! AudioTranscript(nextAudioToSend._2)
  }

  /**
    * Returns a list of files that end in .txt in a given directory
    * @param dir Directory for lookup
    * @return List of file names ending in .txt
    */
  private def getListOfFiles(dir: String): List[String] = {
    val file = new File(dir)
    file.listFiles.filter(_.isFile)
      .filter(_.getName.endsWith(".txt"))
      .map(_.getPath).toList
  }

  /**
    * Convert the given filename into a Map containing the first word of each line and the rest of the sentence
    * @param filename File to open and transform
    * @return Map of form filename => transcript
    */
  def readTextFile(filename: String): Map[String, String] = {
    val pairs =
      for {
        line <- Source.fromFile(filename).getLines()
        split = line.split(" ", 2)
        name = split.head
        sentence = split(2)
      } yield (name -> sentence)
    pairs.toMap
  }

}
