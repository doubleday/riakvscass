package net.wooga.loadclient

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.routing.RoundRobinRouter

object MasterControl {
  case object Start
  case object Stop
}

class MasterControl(connectionFactory: DbConnection.Factory) extends Actor {

  import MasterControl._

  val log = Logging(context.system, this)

  def receive = {
    case Start => {
      context.actorOf(Props(new DbAccessor(connectionFactory())).withRouter(RoundRobinRouter(nrOfInstances = 10)), "DbAccessor")
      (0 until 1000).foreach { i =>
        val user = context.actorOf(Props(new User("Name")))
        user ! User.Wakeup
      }
      sender ! 'done
    }
    case Stop => {
      context.system.shutdown()
    }
  }

}
