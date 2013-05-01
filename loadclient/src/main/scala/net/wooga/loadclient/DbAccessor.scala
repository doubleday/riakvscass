package net.wooga.loadclient

import akka.actor.Actor
import scala.util.Success
import akka.event.Logging

object DbAccessor {
  case class Read(corrId: Long, key: String)
  case class Write(corrId: Long, key: String, value: String)
  case class Response(corrId: Long, key: String, value: String)
  case class Failure(corrId: Long, error: String)
}

class DbAccessor(dbAccess: DbConnection) extends Actor {

  import DbAccessor._

  val log = Logging(context.system, this)

  override def postStop() = {
    log.info("shutdown")
    dbAccess.shutdown()
  }

  override def preStart() {
    log.info("start")
  }

  def receive = {
    case Read(cid, key) => {
      log.debug("Received Read for {} from {} ", key, sender)
      sender ! (
        dbAccess.read(key) match {
          case Success(str) => Response(cid, key, str)
          case scala.util.Failure(error) => Failure(cid, "" + error)
        })
    }
    case Write(cid, key, value) => {
      sender ! (
        dbAccess.write(key, value) match {
          case Success(_) => Response(cid, key, value)
          case scala.util.Failure(error) => Failure(cid, "" + error)
        })
    }
  }

}
