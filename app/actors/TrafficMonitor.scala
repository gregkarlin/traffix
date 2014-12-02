package actors

import akka.actor.{Props, Actor}
import scala.concurrent.duration._

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
  import context.dispatcher

  def receive = {

    case Start() =>
      context.system.scheduler.schedule(0 milliseconds, 5000 milliseconds, trafficClient, TrafficClient.GetHosts())

    case TrafficClient.Hosts(hosts) =>
      hosts.map( host => trafficClient ! TrafficClient.GetHostTraffic(host))
  }

}
