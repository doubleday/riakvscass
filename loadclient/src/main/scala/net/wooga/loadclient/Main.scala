package net.wooga.loadclient

import akka.actor.Props
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import net.wooga.loadclient.DbAccessor.{Failure, Response, Read}
import scala.util.Random


object Main extends App {

  implicit val system = ActorSystem("LoadTest")
  val masterControl = system.actorOf(Props(new MasterControl(DbConnection.riak)), "MasterControl")

  if (args.length > 0) {
    masterControl ! MasterControl.Start


    if (args(0) == "CreateUsers") {
      masterControl ! MasterControl.CreateUsers(Integer.parseInt(args(1)))

    } else if (args(0) == "RunLoadTest") {
      masterControl ! MasterControl.RunLoadTest(Integer.parseInt(args(1)))

    }

    system.awaitTermination()
  }

}
