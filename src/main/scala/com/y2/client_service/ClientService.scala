package com.y2.client_service

import akka.actor.{Actor, ActorLogging, ActorPath, ActorSystem, Props}
import akka.cluster.client.{ClusterClient,ClusterClientSettings}
import com.typesafe.config.ConfigFactory

object ClientService extends MessageSequence with Actor with ActorLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=4551").withFallback(ConfigFactory.load())
    val system = ActorSystem("ScalaClusterClientSystem", config)

    val initialContacts = Set(
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/receptionist"),
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2552/user/receptionist"))
    val c = system.actorOf(ClusterClient.props(
      ClusterClientSettings(system).withInitialContacts(initialContacts)), "os-client")

    c ! ClusterClient.Send("/user/serviceA", "hello", localAffinity = true)
    c ! ClusterClient.SendToAll("/user/serviceB", "hi")
  }

  def receive = {
    case _ => {

    }
  }
}
