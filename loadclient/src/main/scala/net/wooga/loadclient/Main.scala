package net.wooga.loadclient

import akka.actor.Props
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import net.wooga.loadclient.DbAccessor.{Failure, Response, Read}
import scala.util.Random


object Main extends App {

  implicit val system = ActorSystem("LoadTest")
  val masterControl = system.actorOf(Props(new MasterControl(DbConnection.riak)), "MasterControl")


  val testActor = actor(new Act {
    become {
      case 'start => {
        masterControl ! MasterControl.Start
      }
      case 'done => {
        val dbAccessor = system.actorFor("akka://LoadTest/user/MasterControl/DbAccessor")
        println("Calling ... " + dbAccessor)
        dbAccessor ! Read(Random.nextLong(),"Name")
      }
      case Response(cid, key, value) => println("Received resonse for key=" + key + " value=" + value)
      case Failure(cid, error) => println("Received error: " + error)
      case _ => println("Message not understood")
    }
  })

  testActor ! 'start

  Thread.sleep(200000)
//
  masterControl ! MasterControl.Stop

}
