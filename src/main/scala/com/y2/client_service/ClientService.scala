package com.y2.client_service

import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.{Cluster, ClusterEvent}
import com.y2.messages.Message._
import akka.cluster.ClusterEvent.MemberUp
import com.y2.config.Y2Config
import com.github.tototoshi.csv._

class ClientService extends Actor with ActorLogging {

  /**
    * The y2 cluster.
    */
  private val cluster = Cluster(context.system)

  /**
    * The list of all registered nodes of the y2 cluster.
    */
  private var nodes: Set[ActorRef] = Set()

  private var indexToNode: Map[Int, ActorRef] = _

  /**
    * The list of nodes that are finished.
    */
  private var finishedNodes: Set[ActorRef] = Set()

  /**
    * Whether registration of nodes is still opened.
    */
  private var isRegistrationOpen = true

  /**
    * Create the logs directory if it does not exist.
    */
  new File("logs/").mkdirs()

  /**
    * Base name of the csv files to write the data to.
    */
  private val outputFileBaseName = "logs/" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Calendar.getInstance().getTime)

  /**
    * Name of the file that will contain the finishing times.
    */
  private val clusterEventsCsvFileWriter = CSVWriter.open(new File(outputFileBaseName + "_cluster_events.csv"))
  /**
    * Name of the file that will contain the runtime data.
    */
  private val runtimeDataCsvFileWriter = CSVWriter.open(outputFileBaseName + "_runtime.csv")

  runtimeDataCsvFileWriter.writeRow(List("node", "sampleCount", "decompressionMilli", "trainingMilli", "compressionMilli", "loss", "timestamp"))
  clusterEventsCsvFileWriter.writeRow(List("eventType", "node", "timestamp", "description"))

  /**
    * When the actor starts it tries to join the cluster.
    * We use cluster bootstrap that automatically tries to discover nodes of the cluster and create a new cluster if
    * none was found.
    */
  override def preStart(): Unit = cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[MemberUp])

  /**
    * Unsubscribe from the cluster when stopping the actor.
    */
  override def postStop(): Unit = cluster.unsubscribe(self)

  /**
    * Handle received messages.
    * @return a function that handles the received messages.
    */
  @Override
  def receive = {
    case ClientRequest =>
      if (isRegistrationOpen) {
        log.info("Node " + sender() + " registered.")
        clusterEventsCsvFileWriter.writeRow(List("nodeRegistered", sender(), Instant.now))
        nodes = nodes + sender()
        sender ! ClientAnswer
        if (nodes.size >= Y2Config.config.nodeCount) {
          log.info("All nodes registered to the client! Sending node index which will start the training.")
          isRegistrationOpen = false
          clusterEventsCsvFileWriter.writeRow(List("registrationCompleted", this, Instant.now))
          var count = 0
          nodes.foreach(node => {
            node ! NodeIndex(count, Y2Config.config.nodeCount)
            count = count + 1
          })
        }
      }

    case Finished(timestamp) =>
      finishedNodes = finishedNodes + sender()
      clusterEventsCsvFileWriter.writeRow(List("nodeFinished", sender(), timestamp.toString))
      if (finishedNodes.size == nodes.size) {
        log.info("y2 Cluster finished the training! Congrats!")
        clusterEventsCsvFileWriter.writeRow(List("clusterFinished", this, Instant.now))
        // TODO: Stop the whole cluster.
      }

    case Runtime(sampleCount, decompressionMilli, trainingMilli, compressionMilli, loss) =>
      log.info(s"""Received stats from $sender(): processed $sampleCount samples, took $decompressionMilli [ms] to decompress, $trainingMilli [ms] to train and $compressionMilli [ms] to compress; loss was $loss.""")
      runtimeDataCsvFileWriter.writeRow(List(sender(), sampleCount, decompressionMilli, trainingMilli, compressionMilli, loss, Instant.now))
  }
}
