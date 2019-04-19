package com.y2

import com.y2.config.Y2Config
import com.y2.runtype.Y2Node
import com.y2.runtype.RunTypes.{RunType, Client, Node, Null}
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
    parser.parse(args, Y2Config()) map { config =>
      config.runType match {
        case Client => client()
        case Node => node(config)
        case Null => fail()
      }
    } getOrElse {
      fail()
    }
  }

  /**
    * Executed when invalid arguments are received.
    */
  def fail(): Unit = {
  }

  /**
    * Start the y2 client.
    */
  def client() = {
    println("Running the client")
  }

  /**
    * Start an y2 node.
    * @param c
    */
  def node(c: Y2Config): Unit = {
    if (c.local) {
      new Y2Node(c)
      new Y2Node(c)
      new Y2Node(c)
    } else {
      new Y2Node(c)
    }
  }
}
