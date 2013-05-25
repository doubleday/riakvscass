package net.wooga.loadclient

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import akka.event.Logging
import akka.routing.RoundRobinRouter
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.util.Random

object DbAccessor {
  case class Read(corrId: Long, key: String)
  case class Write(corrId: Long, key: String, value: String)
  case class Response(corrId: Long, key: String, value: String)
  case class Failure(corrId: Long, error: String)

  case class VerifyHost(hostName: String)
}

class DbAccessor(connectionFactory: DbConnection.Factory, hosts: List[String]) extends Actor {

  import ExecutionContext.Implicits.global
  import DbAccessor._

  case class CheckHost(hostName: String)

  val log = Logging(context.system, this)

  var proxies: Map[String, ActorRef] = null
  var checkedHosts: Set[String] = Set()

  override def preStart() {

    proxies = hosts.flatMap { host =>
      connectionFactory(host) match {
        case Some(factory) => factory.shutdown(); Some((host, startProxy(host)))
        case None          => scheduleCheck(host); None
      }
    }.toMap

    log.info("start")
  }

  def startProxy(host: String): ActorRef =
    context.actorOf(Props(new DBProxy(connectionFactory(host))).withRouter(RoundRobinRouter(nrOfInstances = 10)), "DBProxy-" + host)

  def scheduleCheck(host: String) = {
    context.system.scheduler.scheduleOnce(Duration(10, TimeUnit.SECONDS), self, CheckHost(host))
    checkedHosts = checkedHosts + host
  }

  def receive = {
    case VerifyHost(host) => {
      if (!checkedHosts.contains(host)) {
        scheduleCheck(host)
      }
    }
    case CheckHost(host) => {
      connectionFactory(host) match {
        case Some(factory) => {
          log.info("Host is online: " + host)
          factory.shutdown()
          addHost(host)
        }
        case None => {
          log.info("Host is offline: " + host)
          removeHost(host)
          scheduleCheck(host)
        }
      }
    }
    case Read(cid,key) => {
      log.debug("Received Read for {} from {} ", key, sender)
      sendToProxy(cid, DBProxy.Read(sender,cid,key))
    }
    case Write(cid,key,value) => {
      log.debug("Received Write for {} from {} ", key, sender)
      sendToProxy(cid, DBProxy.Write(sender,cid,key,value))
    }
  }

  def sendToProxy(cid: Long, message: Product) {
    randomProxy match {
      case Some(proxy) => proxy ! message
      case None => sender ! Failure(cid, "All hosts down")
    }
  }

  def removeHost(host: String) = {
    if (proxies.contains(host)) {
      proxies(host) ! PoisonPill
      proxies = proxies - host
    }
  }

  def addHost(host: String) = {
    if (!proxies.contains(host)) {
      checkedHosts = checkedHosts - host
      proxies = proxies + (host -> startProxy(host))
    }
  }

  def randomProxy: Option[ActorRef] = {
    val ps = proxies
    val hosts = ps.keys.toList

    if (hosts.isEmpty) None
    else Some(ps(hosts(Random.nextInt(hosts.size))))
  }
}
