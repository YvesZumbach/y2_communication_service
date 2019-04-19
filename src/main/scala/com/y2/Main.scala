package com.y2

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.y2.communication_service.CommunicationService
import com.y2.client_service.ClientService
import com.y2.config.Y2Config
import com.y2.messages.ClientCommunicationMessage.ClientRequest
import com.y2.runtype.{Client, Node, Null}
import scopt.OptionParser

/**
  * The main entry point of the y2 cluster.
  */
class Main { }

object Main {

  val parser = new OptionParser[Y2Config]("y2") {
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
      case Some(c) => {
        c.runType match {
          case Null => fail()
          case Client => client()
          case Node => node(c)
        }
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
  def client() = {
    println("Running the client")
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
  def node(c: Y2Config): Unit = {
    if (c.local) {
      for (i <- 1 to c.localNodeCount) {
        // Use special configuration in order to run several nodes on one computer
        val config: Config = ConfigFactory.parseString(s"""
          akka.remote.artery.canonical.hostname = "127.0.0.$i"
          akka.management.http.hostname = "127.0.0.$i"
          akka.remote.artery.canonical.port = 255$i
        """).withFallback(ConfigFactory.load())
        val system = ActorSystem("y2", config)
        system.actorOf(Props[CommunicationService], "communication")
      }
    } else {
      // Use default configuration
      val system = ActorSystem("y2")
      system.actorOf(Props[CommunicationService], "communication")
    }
  }
}
