package net.wooga.loadclient

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import net.wooga.loadclient.DbAccessor.VerifyHost
import scala.util.Success

object DBProxy {
  case class Read(client: ActorRef, corrId: Long, key: String)
  case class Write(client: ActorRef, corrId: Long, key: String, value: String)
}

class DBProxy(dbAccessOption: Option[DbConnection]) extends Actor {
  import DBProxy._

  val log = Logging(context.system, this)
  val dbAccess = dbAccessOption.getOrElse {throw new IllegalStateException("connection dead")}

  log.info("DBProxy: New proxy with connection " + dbAccess)

  override def postStop() = {
    log.info("shutdown")
    dbAccess.shutdown()
  }

  def receive = {
    case Read(client, cid, key) => {
      client ! (
        dbAccess.read(key) match {
          case Success(str) => DbAccessor.Response(cid, key, str)
          case scala.util.Failure(error) => {
            sender ! VerifyHost(dbAccess.hostName)
            DbAccessor.Failure(cid, "" + error)
          }
        })
    }
    case Write(client, cid, key, value) => {
      client ! (
        dbAccess.write(key, value) match {
          case Success(_) => DbAccessor.Response(cid, key, value)
          case scala.util.Failure(error) => {
            sender ! DbAccessor.VerifyHost(dbAccess.hostName)
            DbAccessor.Failure(cid, "" + error)
          }
        })
    }
  }

}
