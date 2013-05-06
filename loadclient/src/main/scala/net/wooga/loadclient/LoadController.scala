package net.wooga.loadclient

import akka.actor.{Props, Actor}

object LoadController {

  case class StartTest(concurrentUsers: Int)
}

class LoadController extends Actor {

  import LoadController._

  val maxUser = 5000000
  var nextUserId = 0

  def nextUserName() = {
    nextUserId += 1
    if (nextUserId > maxUser) nextUserId = 0
    nextUserId.toString
  }

  def forkNewUser() = {
    val user = context.actorOf(Props(new User(nextUserName())))
    user ! User.Wakeup
  }

  def receive = {

    case StartTest(numUsers) => {
      (0 until numUsers).foreach( (i) => {
        forkNewUser()
        Thread.sleep(10)
      })
    }

    case User.Logout => {
      Stats.loginCounter.inc(1)
      forkNewUser()
    }
  }
}
