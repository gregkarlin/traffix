package actors

import akka.actor.{Props, Actor}
import scala.concurrent.duration._
import play.api.Play.current

object TrafficMonitor {
  case class Start()
}

/**
 * Schedules Chartbeat requests to monitor traffic
 */
class TrafficMonitor extends Actor {

  import TrafficMonitor._
  //implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  val trafficClient = context.actorOf(Props[TrafficClient])
  val intervalMS = current.configuration.getLong("monitor.interval").get
  import context.dispatcher

  def receive = {

    case Start() =>
      context.system.scheduler.schedule(0 milliseconds, intervalMS milliseconds, trafficClient, TrafficClient.GetHosts())

    case TrafficClient.Hosts(hosts) =>
      hosts.map( host => trafficClient ! TrafficClient.GetHostTraffic(host))
  }

}
