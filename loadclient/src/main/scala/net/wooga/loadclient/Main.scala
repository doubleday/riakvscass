package net.wooga.loadclient

import akka.actor.Props
import akka.actor.ActorDSL._
import akka.actor.ActorSystem


object Main extends App {

  if (args.length > 0) {

    val target  = args(0)
    val command = args(1)

    implicit val system = ActorSystem("LoadTest")

    val (factory, hosts) = target match {
      case "riak"     => (DbConnection.riak, Config.riakServers)
      case "cassandra" => (DbConnection.cassandra, Config.cassandraServers)
    }

    val masterControl = system.actorOf(Props(new MasterControl(factory, hosts)), "MasterControl")
    masterControl ! MasterControl.Start

    if (command == "CreateUsers") {
      masterControl ! MasterControl.CreateUsers(Integer.parseInt(args(2)))

    } else if (command == "RunLoadTest") {
      masterControl ! MasterControl.RunLoadTest(Integer.parseInt(args(2)))

    }

    system.awaitTermination()

  } else {
    System.err.println("Nothing to do")
  }

}
