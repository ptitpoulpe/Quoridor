package models

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import models.KDirection._

object KhetServer {
  implicit val timeout = Timeout(1 second)

  def create_or_join():Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val khet = Akka.system.actorOf(Props(new KhetServer("0")))
    (khet ? KJoin("toto")).asPromise.map {
      case KConnected(player) =>
        val iteratee = Iteratee.foreach[JsValue] { event =>
                       (event \ "type").asOpt[String] match {
                         case Some("board") => khet ! KBoard()
                         case Some("mv")    => (event \ "x").asOpt[Int].map(x =>
                                               (event \ "y").asOpt[Int].map(y =>
                                               (event \ "dir").asOpt[List[Int]]
                                                              .map(dir =>
                                               khet ! KMove(x, y, dir(0), dir(1)))))
                         case Some("rt")    => (event \ "x").asOpt[Int].map(x =>
                                               (event \ "y").asOpt[Int].map(y =>
                                               (event \ "dir").asOpt[Boolean]
                                                              .map(cw =>
                                               khet ! KRotate(x, y, cw))))     
                         case None          => {}}}
        (iteratee, player)
      case KCannotConnect(error) =>
        val iteratee = Done[JsValue,Unit]((),Input.EOF)
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))
        (iteratee, enumerator)
    }
  }
}

class KhetServer(id:String) extends Actor {

  var khet = Khet()
  var players = Map.empty[String, (Int, PushEnumerator[JsValue])]

  def receive = {
    case KJoin(username) => {
      if (players.size<2 | players.contains(username)) {
        val player = Enumerator.imperative[JsValue]()
        players += (username -> ((if (players.contains(username))
                                    players(username)._1
                                  else players.size, 
                                  player)))
        sender ! KConnected(player)
        send_board()
      } else {
        sender ! KCannotConnect("Too many players")
      }
    }
    case KBoard() =>
      send_board()
    case KMove(x,y,sx,sy) =>
      khet = khet.move((x,y), (sx, sy))
      send_board()
    case KRotate(x, y, cw) =>
      Logger.info("CW:"+cw)
      khet = khet.rotate((x,y), cw)
      send_board()
  }

  def send_board() = {
    val msg = JsObject(
      Seq(
        "id" -> JsString(id),
        "pawns" -> JsArray(khet.pawns.map({case ((x, y), Pawn(t, c, d)) =>
                           JsArray(List(JsBoolean(c),
                                        JsNumber(t),
                                        JsArray(List(JsNumber(x),JsNumber(y))),
                                        JsNumber(d)))}
                           ).toList),
        "beam" -> JsArray(khet.beam.toList.map({case ((x,y),d) =>
                          val (dx, dy) = dir2pos(d)
                          JsArray(List(JsNumber(x),
                                       JsNumber(y),
                                       JsNumber(dx),
                                       JsNumber(dy)))})),
        "killed" -> JsArray(khet.kpawns.map({case ((x,y), Pawn(t, c, d)) =>
                            JsArray(List(JsBoolean(c),
                                         JsNumber(t),
                                         JsArray(List(JsNumber(x),JsNumber(y))),
                                         JsNumber(d)))}
                            ).toList)
      )
    )
    players.foreach {case (username, (i, player)) => 
                      player.push(msg)}
  }

}

case class KConnected(enumerator:Enumerator[JsValue])
case class KCannotConnect(msg: String)

case class KJoin(username:String)
case class KBoard()
case class KMove(x:Int, y:Int, sx:Int, sy:Int)
case class KRotate(x:Int, y:Int, cw:Boolean)
