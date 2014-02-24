package de.examples

import akka.actor.{Props, Actor, ActorSystem}
import akka.routing.RoundRobinPool

/**
 * Created by christian on 2/24/14.
 */
object Main extends App {

  println("AKKA Round Robin Eventbus Example")

  val system = ActorSystem("akka-robin-bus")
  
  val supervisor = system.actorOf(Props[Supervisor])
  
  supervisor ! "start"
  
}

// communication classes
trait Msg
case class StrMsg(str: String) extends Msg
case class IntMsg(int: Int) extends Msg

class Supervisor extends Actor {
  
  def receive = {
    case "start" =>
    println("Supervisor will start...")
    
    val strRouter = context.actorOf(Props[RoundRobinEventBusSubscriber[StrSubscriber]])
    val intRouter = context.actorOf(Props[RoundRobinEventBusSubscriber[IntSubscriber]])

    println("wait two seconds before publishing...")
    Thread.sleep(2000)
    println("continue")

    for (i <- 1 to 50) context.system.eventStream.publish(StrMsg("STR MSG NO %d" format i))
    for (i <- 1 to 25) context.system.eventStream.publish(IntMsg(i))
    
  }
  
}

class RoundRobinEventBusSubscriber[T <: Actor] extends Actor {
  
  private val routees = context.actorOf(RoundRobinPool(5).props(Props[StrSubscriber]))

  override def preStart {
    super.preStart
    context.system.eventStream.subscribe(context.self, classOf[Msg])
  }

  override def postStop {
    context.system.eventStream.unsubscribe(context.self, classOf[Msg])
    super.postStop
  }

  def receive = { case msg: Msg => routees ! msg }
  
}

class StrSubscriber extends Actor {
  
  def receive = {
    case StrMsg(str) => println("StrSubscriber %s receives %s" format (self.path.name, str))
  }
  
}

class IntSubscriber extends Actor {

  def receive = {
    case IntMsg(int) => println("IntSubscriber %s receives %d" format (self.path.name, int))
  }

}