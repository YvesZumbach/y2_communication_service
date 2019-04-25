package com.y2

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.y2.communication_service.CommunicationService
import com.y2.client_service.ClientService
import com.y2.config.Y2Config
import com.y2.messages.ToWorker
import com.y2.runtype.{Client, Node, Null}
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

    cmd("node")
      .text("An y2 cluster work-horse, also known as 'a node'.")
      .action { (_, c) => c.copy(runType = Node) }
      .children(
        opt[Boolean]("local")
          .abbr("l")
          .action((x, c) => c.copy(local = x))
          .text("if true, a local cluster of three nodes will be started, otherwise, start just on node."),
        opt[Int]("localNodeCount")
          .action((x, c) => c.copy(localNodeCount = x))
          .text("the number of local node to run.")
      )

    checkConfig(c => c.runType match {
      case Null => failure("You must specify a subcommand.")
      case _ => success
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
      case Some(c) => c.runType match {
        case Null => fail()
        case Client => client()
        case Node => node(c)
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
    val config: Config = ConfigFactory.parseString(s"""
        akka.cluster.roles = ["client"]
        """).withFallback(ConfigFactory.load())
    val system = ActorSystem("y2", config)
    val client = system.actorOf(Props[ClientService], "client")
  }

  /**
    * Start an y2 node.
    * @param c
    */
  private def node(c: Y2Config): Unit = {
    if (c.local) {
      println("Running a local y2 node.")
      val urls = for (i <- 1 to c.localNodeCount) yield "\"akka://y2@127.0.0.1:255" + i + "\"\n"
      val seedNodes = "akka.cluster.seed-nodes = [\n" + urls.reduce(_ + _) + "]\n"
      // Create all systems first
      val systems = for {
        i <- 1 to c.localNodeCount
        // Use special configuration in order to run several nodes on one computer
        config: Config = ConfigFactory.parseString(s"""
          akka.management.http.hostname = "127.0.0.1"
          akka.remote.artery.canonical.hostname = "127.0.0.1"
          akka.remote.artery.canonical.port = 255$i
        """ + seedNodes).withFallback(ConfigFactory.load())
      } yield ActorSystem("y2", config)
      // Create a communication actor for each of the systems
      systems.foreach(_.actorOf(Props[CommunicationService], "communication"))
    } else {
      println("Running an y2 node.")
      // Use default configuration
      val system = ActorSystem("y2")
      system.actorOf(Props[CommunicationService], "communication")
    }
  }
}
