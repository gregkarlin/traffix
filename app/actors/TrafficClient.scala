package actors

import akka.actor.{Actor,Props}
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play.current
import play.api.mvc._
import play.api._

object TrafficClient {
  case class GetHosts()
  case class GetHostTraffic(host: String)
  case class Hosts(hosts: List[String])
  case class TrafficData(i: String, path: String, visitors: Int)
  case class HostTrafficData(host: String, trafficData: List[TrafficData])

  implicit val TrafficDataFormat = Json.format[TrafficData]
  implicit val HostsFormat = Json.format[Hosts]

  val apiUrl = "http://api.chartbeat.com/live/toppages/?apikey=317a25eccba186e0f6b558f45214c0e7&"
  val hostsUrl = "https://s3.amazonaws.com/interview-files/hosts.json"
  val apiLimit = 100

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  /**
   * Makes an asynchronous call to chartbeat api for given domain
   */
  def getCurrentTraffic(host: String)  = {
    val url = s"$apiUrl&host=$host&limit=$apiLimit"
    WS.url(url).get().map {
      response =>
        response.json.as[List[TrafficData]]
    }
  }
}

/**
 * Responsible for retrieving traffic data from chartbeat api
 */
class TrafficClient extends Actor {

  import TrafficClient._
  import scala.concurrent.ExecutionContext.Implicits.global
  val dataClient = context.actorOf(Props[DataStoreClient])
  def receive = {

    case GetHosts() =>
      val monitor = sender
      val url = hostsUrl
      WS.url(url).get().map {
        response =>
          val hosts = response.json.as[List[String]]
          monitor ! Hosts(hosts)
      }


    case GetHostTraffic(host) =>
      val url = s"$apiUrl&host=$host&limit=$apiLimit"
      WS.url(url).get().map {
        response =>
          val trafficData = response.json.as[List[TrafficData]]
          dataClient ! HostTrafficData(host, trafficData)
      }

  }

}
