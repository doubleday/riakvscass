package net.wooga.loadclient

import akka.actor.{ActorContext, ActorRef}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.language.implicitConversions

object DeferredSender {
  implicit def actorToDeferredSender[A <: ActorRef](actor: A) = new DeferredSender(actor)
}

class DeferredSender[A <: ActorRef](actor: A) {
  def sendLater(message: Any, millis: Long = 1)(implicit context: ActorContext) = {
    context.system.scheduler.scheduleOnce(Duration(millis, TimeUnit.MILLISECONDS), actor, message)(context.dispatcher)
  }
}
