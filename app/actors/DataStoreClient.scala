package actors

import akka.actor.Actor
import com.redis._
import util.Util.getCurrentTime
import play.api.libs.json._
import play.api.Play.current

object DataStoreClient {
  case class HostPathData(path:String, i: String, delta: Long, absAvg: Long, movAvg: Long, momentum: Long)
  case class GetHostData(host: String)
  case class HostData(data: String)

  val redisHost = current.configuration.getString("redis.host").get
  val redisPort = current.configuration.getInt("redis.port").get
  val windowSize = current.configuration.getInt("redis.windowsize").get
  val client = new RedisClient(redisHost, redisPort)
  implicit val hostPathDataFormat = Json.format[HostPathData]

  def getHostData(host: String) = {
    val hostKey = s"$host:data"
    val hostData = (client lrange (hostKey,0,-1)).get
    Json.toJson(hostData)
    //val serializedData = hostData.foldLeft("[")((a: String, b: Option[String]) => a + "," + b.get) + "]"
    //serializedData
  }
}

class DataStoreClient extends Actor{

  import DataStoreClient._

  private def getCount(keyCounter: String) = (client incr (keyCounter, 1)).get

  private def getLast(key: String) = (client lrange (key, -1, -1)).get(0).get.toInt

  private def getAbsAvg(absAvgKey: String, delta: Long, count: Long) = {
    val lastAbsAvg = if (client exists absAvgKey) (client get (absAvgKey)).get.toInt else delta
    ((lastAbsAvg * (count - 1)) + delta) / count
  }

  private def getMovAvg(count:Long, absAvg: Long, delta: Long, movAvgKey: String, deltaKey: String) = {
    if (count > windowSize) {
      val lastMovAvg = (client get (movAvgKey)).get.toInt
      val removeDelta = (client lpop (deltaKey)).get.toInt
      ((lastMovAvg * (count - 1)) - removeDelta + delta) / count
    }
    else  absAvg
  }

  private def updateHostPath(host: String, trafficData: TrafficClient.TrafficData, index: Int) {
    val key = s"$host${trafficData.path}"
    val deltaKey = s"$key:delta"
    val keyCounter = s"$key:counter"
    val absAvgKey = s"$key:absAvg"
    val movAvgKey = s"$key:movAvg"
    val hostKey = s"$host:data"
    if (!(client exists key)) {
      client set (s"$key:creation", getCurrentTime)
      client set (s"$key:title", trafficData.i)
      client set (keyCounter, 0)
    }
    else {
      //increment counter
      val count = getCount(keyCounter)
      val last = getLast(key)
      //calc delta
      val delta = trafficData.visitors - last
      //calc absolute average delta
      val absAvg = getAbsAvg(absAvgKey, delta, count)
      val movAvg = getMovAvg(count, absAvg, delta, movAvgKey, deltaKey)
      val momentum = movAvg * trafficData.visitors
      //TODO calc sparseness, max, min
      val dataPoint = HostPathData(trafficData.path, trafficData.i, delta, absAvg, movAvg, momentum)
      val serializedData = Json.toJson(dataPoint).toString
      val hostDataLength = (client llen hostKey).get
      if (index >= hostDataLength) client rpush (hostKey,serializedData)
      else client lset (hostKey,index,serializedData)
      client set (movAvgKey, movAvg)
      client set (absAvgKey, absAvg)
      client rpush (deltaKey, delta)
    }
    client rpush (key,trafficData.visitors)

  }



  def receive = {

    case TrafficClient.HostTrafficData(host, trafficData) =>
      trafficData.zipWithIndex.foreach { case (element, index) => updateHostPath(host, element, index) }

    case GetHostData(host) =>
      val hostKey = s"$host:data"
      val hostData = (client lrange (hostKey,0,-1)).get
      val serializedData = hostData.foldLeft("[")((a: String, b: Option[String]) => a + b.get) + "]"
      sender ! HostData(serializedData)

  }

}
