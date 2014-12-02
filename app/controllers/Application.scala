package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import models._
import actors._
import scala.concurrent.Future
import play.api.libs.json._
import akka.actor.{ActorSystem, Props}

object Application extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  val system = ActorSystem()

  def getHostData(host: String) = Action {
    Ok(DataStoreClient.getHostData(host))
  }
}