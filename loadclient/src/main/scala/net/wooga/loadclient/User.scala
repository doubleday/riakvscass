package net.wooga.loadclient

import akka.actor.{Actor, FSM}
import scala.concurrent.duration.{FiniteDuration, Duration}
import java.util.concurrent.TimeUnit
import scala.util.Random
import DeferredSender._

object User {

  // received events
  case object Wakeup
  case object Read
  case object Write

  // sent events
  case object Logout
  case object LogoutAfterError

  // states
  sealed trait State
  case object Idle extends State
  case object Waiting extends State
  case object Writing extends State
  case object Reading extends State

  // data
  sealed trait Data
  case object Uninitialized extends Data
  case object Error extends Data
  case class Correlation(cid: Long, tstamp: Long = System.currentTimeMillis()) extends Data
  case class WriteBack(cid: Long, data: String, tstamp: Long = System.currentTimeMillis()) extends Data

}

import User._
import DbAccessor._

class User(userId: String) extends Actor with FSM[State, Data] {

  val timeout = Duration(5, TimeUnit.SECONDS)

  var remainingRequests = 10

  lazy val dbAccessor = context.actorFor("akka://LoadTest/user/MasterControl/DbAccessor")

  def now = System.currentTimeMillis

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Wakeup, Uninitialized) =>
      goto(Reading) using Correlation(Random.nextLong())
  }

  when(Reading, stateTimeout = timeout) {
    case Event(Response(rid, key, value), Correlation(cid, tstamp)) if (rid == cid) => {
      log.debug("Received response with {}", cid)
      Stats.readCounter.inc(1)
      Stats.readHisto.update(now - tstamp)
      goto(Writing) using WriteBack(Random.nextLong(),value)
    }
    case Event(Failure(rid, error), Correlation(cid, tstamp)) if (rid == cid) => {
      Stats.errorCounter.inc(1)
      goto(Idle) using Error
    }
    case Event(StateTimeout, Correlation(cid, tstamp)) => {
      Stats.timeoutCounter.inc(1)
      goto(Idle) using Error
    }
    case e:Event => {
      log.debug("Received old response {}", e)
      stay()
    } // late event
  }

  when(Writing, stateTimeout = timeout) {
    case Event(Response(rid, key, value), WriteBack(cid,_,tstamp)) if (rid == cid) => {
      log.debug("Received response with {}", cid)
      Stats.writeCounter.inc(1)
      Stats.writeHisto.update(now - tstamp)
      goto(Idle) using Uninitialized
    }
    case Event(Failure(rid, error), Correlation(cid, tstamp)) if (rid == cid) => {
      Stats.errorCounter.inc(1)
      goto(Idle) using Error
    }
    case Event(StateTimeout, Correlation(cid, tstamp)) => {
      Stats.timeoutCounter.inc(1)
      goto(Idle) using Error
    }
    case e:Event => {
      log.debug("Received old response: {}", e )
      stay()
    } // late event
  }

  onTransition {
    case Idle -> Reading =>
      (nextStateData: @unchecked) match {
        case Correlation(cid,ts) => {
          log.debug("Sending read request with {}", cid)
          dbAccessor ! DbAccessor.Read(cid, userId)
        }
      }
    case Reading -> Writing => {
      (nextStateData: @unchecked) match {
        case WriteBack(cid,value,ts) => {
          val userValue = if (value != null) value else ContentInitializer.userData
          log.debug("Sending write request with {}", cid)
          dbAccessor ! DbAccessor.Write(cid, userId, userValue)
        }
      }
    }
    case _ -> Idle => {
      (nextStateData: @unchecked) match {
        case Error => {
          context.parent ! LogoutAfterError
        }
        case Uninitialized => {
          remainingRequests -= 1
          if (remainingRequests > 0)
            self.sendLater(Wakeup, (10 + Random.nextInt(30)) * 1000)

          else {
            context.parent ! Logout
          }
        }
//        case Response => {
//          log.debug("olad response")
//        }
      }
    }
  }

  initialize
}
