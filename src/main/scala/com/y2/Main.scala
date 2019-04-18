package com.y2

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger
import com.y2.communication_service.CommunicationService
import picocli.CommandLine
import picocli.CommandLine.Command

import scala.collection.JavaConverters

/**
  * The main entry point of the y2 cluster.
  */
class Main { }

@Command(
  name = "y2",
  descriptionHeading = "One command to rule them all in the y2 cluster.",
  subcommands = Array(
    classOf[CommunicationService]
  )
)
object Main {

  val log = Logger(classOf[Main])

  implicit val system = ActorSystem("y2")

  def main(args: Array[String]): Unit = {

    val commandLine = new CommandLine(Main)

    val parsed = JavaConverters.iterableAsScalaIterable(commandLine.parse(args.toString)).toList
    handleParseResult(parsed)
  }

  def handleParseResult(parsed: List[CommandLine]) = {
    if (parsed.length == 1) {
      log.info("No command was given.")
    } else if (parsed.length != 2) {
      log.info("You can only give one command.")
    } else {
      log.info("Succes!")
    }
  }
}
