package net.wooga.loadclient

import akka.actor.Actor
import scala.util.Random
import akka.event.Logging

object ContentInitializer {

  case class CreateUsers(from: Int, to: Int)

  case object Done
  case object Error
}

class ContentInitializer extends Actor {

  import ContentInitializer._

  val log = Logging(context.system, this)

  lazy val dbAccessor = context.actorFor("akka://LoadTest/user/MasterControl/DbAccessor")

  val queueSize = 100
  var usersToCreate = 0
  var nextUserId = 0

  def nextUserName() = {
    nextUserId += 1
    nextUserId.toString
  }

  def createUser() {
    val userId = nextUserName()
    val userValue: String = Random.alphanumeric.take(2000).mkString
    dbAccessor ! DbAccessor.Write(Random.nextLong(), userId, userValue)
  }

  def receive = {
    case CreateUsers(from, to) => {
      nextUserId    = from
      usersToCreate = to - from
      (from until from + queueSize).foreach( (i) => createUser() )
    }
    case DbAccessor.Response(_,_,_) => {
      usersToCreate -= 1
      if (usersToCreate > 0) createUser()
      else {
        context.parent ! Done
        context.stop(self)
      }
    }
    case DbAccessor.Failure(_,error) => {
      log.error(error)
      context.parent ! Error
      context.stop(self)
    }
  }
}
