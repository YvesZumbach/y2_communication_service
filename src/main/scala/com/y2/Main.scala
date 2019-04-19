package com.y2

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import com.y2.config.Config
import com.y2.runtype.{CLIENT, NODE, NULL, Node}
import scopt.OptionParser

/**
  * The main entry point of the y2 cluster.
  */
class Main { }

object Main extends LazyLogging {

  implicit val system = ActorSystem("y2")

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

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) map { config =>
      config.runType match {
        case CLIENT => client()
        case NODE => node(config)
      }
    } getOrElse {
      logger.error("Invalid arguments! Not doing anything.")
    }
  }

  def client() = {
    println("Running the client")
  }

  def node(c: Config): Unit = {
    new Node(c)
  }
}
