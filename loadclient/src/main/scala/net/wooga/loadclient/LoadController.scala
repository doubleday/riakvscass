package net.wooga.loadclient

import akka.actor.{Props, Actor}
import java.util.concurrent.TimeUnit
import akka.event.Logging
import scala.util.Random

object LoadController {

  case class ConfigureTest(minUser: Int, maxUser: Int, fixedCount: Int)
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
  var fixedCount = -1
  var loggedInUsers = 0

  def nextUserName() = {
    nextUserId += 1
    if (nextUserId > maxUser) nextUserId = minUser
    nextUserId.toString
  }

  def spawnNewUser() = {
    val user = context.actorOf(Props(new User(nextUserName())))
    user ! User.Wakeup
    Stats.userCounter.inc()
    Stats.loginCounter.inc()
    loggedInUsers = loggedInUsers + 1
  }

  def spawnNewUserIfRoom() = {
    if (lastTimeout < System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)) {
      if (Random.nextFloat() < 0.21) spawnNewUser()
    }
  }

  def receive = {

    case ConfigureTest(min,max,fixed) => {
      minUser = min
      nextUserId = minUser
      maxUser = max
      fixedCount = fixed
    }

    case StartTest() => {
      self ! SpawnUsers
    }

    case SpawnUsers => {
      if (fixedCount > 0) {
        if (loggedInUsers < fixedCount) {
          for (i <- 1 to Math.min(fixedCount - loggedInUsers, 25)) spawnNewUser()
          self sendLater SpawnUsers
        }

      } else if (lastTimeout == 0) {
        for (i <- 1 to 25) spawnNewUser()
        self sendLater SpawnUsers
      }
    }

    case User.Logout => {
      Stats.userCounter.dec()
      loggedInUsers = loggedInUsers - 1
      context.stop(sender)
      if (loggedInUsers < fixedCount) {
        spawnNewUser()
      } else {
        spawnNewUser()
        spawnNewUserIfRoom()
      }
    }

    case User.LogoutAfterError => {
      Stats.userCounter.dec()
      loggedInUsers = loggedInUsers - 1
      if (loggedInUsers < fixedCount) {
        spawnNewUser()
      }
      log.debug("Timeout occured")
      context.stop(sender)
      lastTimeout = System.currentTimeMillis()
    }
  }
}
