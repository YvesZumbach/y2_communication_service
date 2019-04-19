package com.y2

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging
import com.y2.client_service.ClientService
import com.y2.config.Config
import com.y2.runtype.{CLIENT, NODE, NULL, Node}
import scopt.OptionParser

/**
  * The main entry point of the y2 cluster.
  */
class Main { }

object Main extends LazyLogging {

  val parser = new OptionParser[Config]("y2") {
    head("y2", "v1.0")

    note("The base command to handle a y2 cluster.")

    cmd("client")
      .text("An y2 cluster entry point, also known as 'a client'.")
      .action { (_, c) => c.copy(runType = CLIENT) }

    cmd("node")
      .text("An y2 cluster work-horse, also known as 'a node'.")
      .action { (_, c) => c.copy(runType = NODE) }

    checkConfig(c => c.runType match {
      case NULL => failure("You must specify a subcommand.")
      case _ => success
    })
  }

  /**
    * main function.
    * @param args The command line arguments
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) map { config =>
      implicit val system = ActorSystem("y2")
      config.runType match {
        case CLIENT => client()
        case NODE => node(config)
        case NULL => fail()
      }
    } getOrElse {
      fail()
    }
  }

  /**
    * Executed when invalid arguments are received.
    */
  def fail(): Unit = {
    logger.error("Invalid arguments! Not doing anything.")
  }

  /**
    * Start the y2 client.
    */
  def client()(implicit system: ActorSystem) = {
    println("Running the client")
    val clientService: ActorRef = system.actorOf(Props[ClientService], "client")
  }

  /**
    * Start an y2 node.
    * @param c
    */
  def node(c: Config)(implicit system: ActorSystem): Unit = {
    new Node(c)
  }
}
