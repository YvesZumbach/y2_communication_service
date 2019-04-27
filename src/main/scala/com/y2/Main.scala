package com.y2

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.y2.communication_service.CommunicationService
import com.y2.client_service.ClientService
import com.y2.config.Y2Config
import com.y2.runtype.{Client, Node, Null}
import com.y2.utils.Utils
import scopt.OptionParser

/**
  * The main entry point of the y2 cluster.
  */
object Main {

  private val parser = new OptionParser[Y2Config]("y2") {
    head("y2", "v1.0")

    note("The base command to handle a y2 cluster.")

    cmd("client")
      .text("An y2 cluster entry point, also known as 'a client'.")
      .action { (_, c) => c.copy(runType = Client) }
        .children(
          opt[Int]("node-count")
            .abbr("n")
            .action{(v, c) => c.copy(nodeCount = v)}
            .text("The number of nodes that will be part of the cluster (needed to compute which node will train on which data).")
        )

    cmd("node")
      .text("An y2 cluster work-horse, also known as 'a node'.")
      .action { (_, c) => c.copy(runType = Node) }
      .children(
        opt[Boolean]("local")
          .abbr("l")
          .action((x, c) => c.copy(local = x))
          .text("If true, a local cluster of three nodes will be started, otherwise, start just on node."),
        opt[Int]("local-node-count")
          .action((x, c) => c.copy(localNodeCount = x))
          .text("The number of local node to run.")
      )

    opt[String]("seed-node")
      .required()
      .abbr("s")
      .action((x, c) => c.copy(seedNode = x))
      .text("The IP address to use to enter the cluster (cannot be 127.0.0.1). If you are starting the first node of th cluster, use the external IP of the node.")

    checkConfig(c => c.runType match {
      case Null => failure("You must specify a subcommand.")
      case _ =>
        if (!c.local && !Utils.isValidIpv4(c.seedNode)) {
          failure("Invalid IPv4 address for seed node parameter. Please set a valid IPv4 for the seed-node parameter.")
        } else {
          c.runType match {
            case Client =>
              if (c.nodeCount <= 0) failure("You must register how many node there is in the y2 cluster (option n)")
              else success
            case _ => success
          }
        }
    })
  }

  /**
    * main function.
    * @param args The command line arguments
    */
  def main(args: Array[String]): Unit = {
    val config = parser.parse(args, Y2Config())
    config match {
      case None => fail()
      case Some(c) =>
        Y2Config.config = c
        c.runType match {
          case Null => fail()
          case Client => client()
          case Node => node()
      }
    }
  }

  /**
    * Executed when invalid arguments are received.
    */
  def fail(): Unit = {
    println("[ERROR] Invalid argument received.")
  }

  /**
    * Start the y2 client.
    */
  private def client(): Unit = {
    println("Running an y2 client.")
    val config: Config = ConfigFactory.parseString(
      "akka.cluster.roles = [\"client\"]\n"
        + Utils.computeArteryHostConfigurationString()
        + Utils.computeSeedNodeConfigurationString(Y2Config.config.seedNode)
    ).withFallback(ConfigFactory.load())
    val system = ActorSystem("y2", config)
    val client = system.actorOf(Props[ClientService], "client")
  }

  /**
    * Start an y2 node.
    */
  private def node(): Unit = {
    val c = Y2Config.config
    if (c.local) {
      println("Running a local y2 node.")
      val urls = for (i <- 1 to c.localNodeCount) yield "\"akka://y2@127.0.0.1:255" + i + "\"\n"
      val seedNodes = "akka.cluster.seed-nodes = [\n" + urls.reduce(_ + _) + "]\n"
      // Create all systems first
      val systems = for {
        i <- 1 to c.localNodeCount
        // Use special configuration in order to run several nodes on one computer
        config: Config = ConfigFactory.parseString(s"""
          akka.remote.artery.canonical.hostname = "127.0.0.1"
          akka.remote.artery.canonical.port = 255$i
        """ + seedNodes).withFallback(ConfigFactory.load())
      } yield ActorSystem("y2", config)
      // Create a communication actor for each of the systems
      systems.foreach(_.actorOf(Props[CommunicationService], "communication"))
    } else {
      println("Running an y2 node.")
      val config: Config = ConfigFactory.parseString(
        "akka.cluster.roles = [\"node\"]\n"
        + Utils.computeArteryHostConfigurationString()
        + Utils.computeSeedNodeConfigurationString(c.seedNode)
      ).withFallback(ConfigFactory.load())
      val system = ActorSystem("y2", config)
      system.actorOf(Props[CommunicationService], "communication")
    }
  }
}
