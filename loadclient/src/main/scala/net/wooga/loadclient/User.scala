package net.wooga.loadclient

import akka.actor.{Actor, FSM}
import scala.concurrent.duration.{FiniteDuration, Duration}
import java.util.concurrent.TimeUnit
import scala.util.Random

object User {

  // events
  case object Wakeup
  case object Read
  case object Write

  // states
  sealed trait State
  case object Idle extends State
  case object Waiting extends State
  case object Writing extends State
  case object Reading extends State

  // data
  sealed trait Data
  case object Uninitialized extends Data
  case class Correlation(cid: Long, tstamp: Long = System.currentTimeMillis()) extends Data

}

import User._
import DbAccessor._

class User(userId: String) extends Actor with FSM[State, Data] {

  lazy val dbAccessor = context.actorFor("akka://LoadTest/user/MasterControl/DbAccessor")

  def now = System.currentTimeMillis

  implicit def toSeconds(secs: Int): FiniteDuration = Duration(secs, TimeUnit.SECONDS)

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Wakeup, Uninitialized) =>
      goto(Reading) using Correlation(Random.nextLong())
  }

  onTransition {
    case Idle -> Reading =>
      nextStateData match {
        case Correlation(cid, ts) => dbAccessor ! DbAccessor.Read(cid, userId)
      }
    case Reading -> Idle =>
      context.system.scheduler.scheduleOnce(10 + Random.nextInt(20), self, Wakeup)(context.dispatcher)
  }

  when(Reading, stateTimeout = 5) {
    case Event(Response(rid, key, value), Correlation(cid, tstamp)) if (rid == cid) => {
      Stats.readHisto.update(now - tstamp)
      goto(Idle) using Uninitialized
    }
    case Event(Failure(rid, error), Correlation(cid, tstamp)) if (rid == cid) => {
      goto(Idle) using Uninitialized
    }
    case Event(StateTimeout, Correlation(cid, tstamp)) => {
      goto(Idle) using Uninitialized
    }
    case _ => stay() // late event
  }


  initialize
}
