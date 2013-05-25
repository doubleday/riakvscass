package net.wooga.loadclient

import akka.actor.{ActorRef, Props, Actor}
import akka.event.Logging
import akka.routing.RoundRobinRouter

object MasterControl {

  case object Start
  case object Stop

  case class CreateUsers(count: Int)
  case class RunLoadTest(concurrentUsers: Int)

}

class MasterControl(connectionFactory: DbConnection.Factory, hosts: List[String]) extends Actor {

  import MasterControl._

  val log = Logging(context.system, this)

  def receive = {
    case Start => {
      context.actorOf(Props(new DbAccessor(connectionFactory, hosts)), "DbAccessor")
    }

    case Stop => {
      context.system.shutdown()
    }

    // create users

    case CreateUsers(count) => {
      log.info("Start creating {} users", count)
      val initializer = context.actorOf(Props[ContentInitializer])
      initializer ! ContentInitializer.CreateUsers(0, count)
    }
    case ContentInitializer.Done => {
      log.info("Finished creating users. Shutting down")
      self ! Stop
    }
    case ContentInitializer.Error => {
      log.info("Error creating users. Shutting down")
      self ! Stop
    }

    // run the load test

    case RunLoadTest(users) => {
      log.info("Starting load test")
      val loadController = context.actorOf(Props[LoadController], "LoadController")
      loadController ! LoadController.StartTest(users)
    }

  }

}
