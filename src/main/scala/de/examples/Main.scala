package de.examples

import akka.actor.{Props, Actor, ActorSystem}
import akka.routing.RoundRobinPool
import scala.reflect.ClassTag

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

/**
 *
 * GIVEN RUNTIME ERROR

[akka-robin-bus-akka.actor.default-dispatcher-3] [akka://akka-robin-bus/user/$a] no matching constructor found on class de.examples.RoundRobinEventBusSubscriber for arguments []
java.lang.IllegalArgumentException: no matching constructor found on class de.examples.RoundRobinEventBusSubscriber for arguments []
	at akka.util.Reflect$.error$1(Reflect.scala:82)
	at akka.util.Reflect$.findConstructor(Reflect.scala:106)
	at akka.actor.NoArgsReflectConstructor.<init>(Props.scala:340)
	at akka.actor.IndirectActorProducer$.apply(Props.scala:289)
	at akka.actor.Props.producer(Props.scala:157)
	at akka.actor.Props.<init>(Props.scala:170)
	at akka.actor.Props$.apply(Props.scala:69)
	at de.examples.Supervisor$$anonfun$receive$1.applyOrElse(Main.scala:33)
	at akka.actor.Actor$class.aroundReceive(Actor.scala:465)
	at de.examples.Supervisor.aroundReceive(Main.scala:27)
	at akka.actor.ActorCell.receiveMessage(ActorCell.scala:491)
	at akka.actor.ActorCell.invoke(ActorCell.scala:462)
	at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:238)
	at akka.dispatch.Mailbox.run(Mailbox.scala:220)
	at akka.dispatch.ForkJoinExecutorConfigurator$AkkaForkJoinTask.exec(AbstractDispatcher.scala:393)
	at scala.concurrent.forkjoin.ForkJoinTask.doExec(ForkJoinTask.java:260)
	at scala.concurrent.forkjoin.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1339)
	at scala.concurrent.forkjoin.ForkJoinPool.runWorker(ForkJoinPool.java:1979)
	at scala.concurrent.forkjoin.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

 *
 */
class RoundRobinEventBusSubscriber[T <: Actor: ClassTag] extends Actor { // <-------------------------------------------
  
  private val routees = context.actorOf(RoundRobinPool(5).props(Props[T])) // <-----------------------------------------

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