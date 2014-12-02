package global

import actors._
import akka.actor.{ActorSystem, Props}

object Global extends play.api.GlobalSettings {

  override def onStart(app: play.api.Application) {
    val system = ActorSystem()
    val monitor = system.actorOf(Props[TrafficMonitor])
    monitor ! TrafficMonitor.Start()

  }

}