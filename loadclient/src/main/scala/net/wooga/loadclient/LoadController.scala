package net.wooga.loadclient

import akka.actor.{Props, Actor}
import java.util.concurrent.TimeUnit
import akka.event.Logging
import scala.util.Random

object LoadController {

  case class ConfigureTest(minUser: Int, maxUser: Int)
  case class StartTest()
}

class LoadController extends Actor {

  import LoadController._
  import DeferredSender._

  case object SpawnUsers

  val log = Logging(context.system, this)

  var minUser = 0
  var maxUser = 1000000
  var nextUserId = 0
  var lastTimeout = 0l

  def nextUserName() = {
    nextUserId += 1
    if (nextUserId > maxUser) nextUserId = minUser
    nextUserId.toString
  }

  def spawnNewUser() = {
    val user = context.actorOf(Props(new User(nextUserName())))
    user ! User.Wakeup
    Stats.userCounter.inc()
  }

  def spawnNewUserIfRoom() = {
    if (lastTimeout < (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10))) {
      if (Random.nextFloat() < 0.21) spawnNewUser()
    }
  }

  def receive = {

    case ConfigureTest(min,max) => {
      minUser = min
      nextUserId = minUser
      maxUser = max
    }

    case StartTest() => {
      self ! SpawnUsers
    }

    case SpawnUsers => {
      if (lastTimeout == 0) {
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
