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
  var khets = Map.empty[String, ActorRef]   

  implicit val timeout = Timeout(1 second)

  def create_or_join(username:Option[String], kid:Option[String]):Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {

    // Get the id of the keth game
    val nkid = kid match { case Some(sid) => Some(sid)
                           case None      => (1 until 100)
                                             .map(_.toString)
                                             .find(!khets.contains(_)) }
    (username, nkid) match {
      case (Some(susername), Some(sid)) => {
        if (!khets.contains(sid)) 
          khets += (sid -> Akka.system.actorOf(Props(new KhetServer(sid))))
        val ks = khets(sid)
        (ks ? KJoin(susername)).asPromise.map {
          case KConnected(player) =>
            val iteratee = Iteratee.foreach[JsValue] { event =>
                          (event \ "type").asOpt[String] match {
                           case Some("board") => ks ! KBoard()
                           case Some("mv")    => (event \ "x").asOpt[Int].map(x =>
                                                 (event \ "y").asOpt[Int].map(y =>
                                                 (event \ "dir").asOpt[List[Int]]
                                                                .map(dir =>
                                                 ks ! KMove(susername,
                                                            x, y, dir(0), dir(1)))))
                           case Some("rt")    => (event \ "x").asOpt[Int].map(x =>
                                                 (event \ "y").asOpt[Int].map(y =>
                                                 (event \ "dir").asOpt[Boolean]
                                                                .map(cw =>
                                                 ks ! KRotate(susername,
                                                              x, y, cw))))     
                           case None          => {}}}
            (iteratee, player)
          case KCannotConnect(error) =>
            val iteratee = Done[JsValue,Unit]((),Input.EOF)
            val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))
            (iteratee, enumerator)
        }
      }
      case whatever => {
        val iteratee = Done[JsValue,Unit]((),Input.EOF)
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString("Server Full")))).andThen(Enumerator.enumInput(Input.EOF))
        Promise.pure((iteratee, enumerator))
      }
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
    case KMove(username, x,y,sx,sy) => players.get(username) match {
      case Some((i, _)) if i==khet.turn =>       
        khet = khet.move((x,y), (sx, sy))
        send_board()
      case whatever => {}
    }
    case KRotate(username, x, y, cw) =>players.get(username) match {
      case Some((i, _)) if i==khet.turn =>
        khet = khet.rotate((x,y), cw)
        send_board()
      case whatever => {}
    }
  }

  def send_board() = {
    val msg = JsObject(
      Seq(
        "id" -> JsString(id),
        "round" -> JsNumber(khet.turn),
        "pawns" -> JsArray(khet.pawns.map({case ((x, y), Pawn(t, c, d)) =>
                           JsArray(List(JsNumber(c),
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
                            JsArray(List(JsNumber(c),
                                         JsNumber(t),
                                         JsArray(List(JsNumber(x),JsNumber(y))),
                                         JsNumber(d)))}
                            ).toList)
      )
    )
    players.foreach {case (username, (i, player)) => 
                      player.push(msg ++ JsObject(
                        Seq("player"  -> JsNumber(i),
                            "turn"    -> JsBoolean(i==khet.turn))))}
  }

}

case class KConnected(enumerator:Enumerator[JsValue])
case class KCannotConnect(msg: String)

case class KJoin(username:String)
case class KBoard()
case class KMove(username:String, x:Int, y:Int, sx:Int, sy:Int)
case class KRotate(username:String, x:Int, y:Int, cw:Boolean)
