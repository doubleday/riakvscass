package net.wooga.loadclient

import akka.actor.{Props, Actor}
import java.util.concurrent.TimeUnit
import akka.event.Logging

object LoadController {

  case class StartTest(concurrentUsers: Int)
}

class LoadController extends Actor {

  import LoadController._
  import DeferredSender._

  case object SpawnUsers

  val log = Logging(context.system, this)

  val maxUser = 1000000
  var numUsers = 0
  var nextUserId = 0
  var lastTimeout = 0l

  def nextUserName() = {
    nextUserId += 1
    (nextUserId % maxUser).toString
  }

  def spawnNewUser() = {
    val user = context.actorOf(Props(new User(nextUserName())))
    user ! User.Wakeup
    Stats.userCounter.inc()
  }

  def spawnNewUserIfRoom() = {
    if (lastTimeout < (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1))) spawnNewUser()
  }

  def receive = {

    case StartTest(users) => {
      numUsers = users
      self ! SpawnUsers
    }

    case SpawnUsers => {
      if (lastTimeout == 0 && nextUserId < numUsers) {
        for (i <- 1 to 25) spawnNewUser()
        self sendLater SpawnUsers
      }
    }

    case User.Logout => {
      Stats.loginCounter.inc(1)
      Stats.userCounter.dec()
      context.stop(sender)

      spawnNewUser()
      spawnNewUserIfRoom()
    }

    case User.LogoutAfterError => {
      log.debug("Timeout occured")
      context.stop(sender)
      Stats.userCounter.dec()
      lastTimeout = System.currentTimeMillis()
    }
  }
}
