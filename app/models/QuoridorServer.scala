package models

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.akka._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise

import play.api.Play.current
import models.QDirection._
import models.QOrientation._

object QuoridorServer {
  def create():Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val qs = Akka.system.actorOf(Props[QuoridorServer])
    val iteratee = Iteratee.foreach[JsValue] { event => 
     (event \ "type").asOpt[String] match {
       case Some("board") => qs ! QBoard()
       case Some("move")  => (event \ "x").asOpt[Int].map(x =>
                             (event \ "y").asOpt[Int].map(y =>
                                qs ! QMove((x,y))))
       case Some("wall")  => (event \ "x").asOpt[Int].map(x =>
                             (event \ "y").asOpt[Int].map(y =>
                             (event \ "ori").asOpt[Boolean].map(ori =>
                                qs ! QWall(x, y, ori))))
       case None          => {}
     }
    }
    (qs ? (QJoin(), 1 second)).asPromise.map {
      case QConnected(player) => 
      {Logger("quoridor").info("connected");(iteratee, player)}
    }
  }
}

class QuoridorServer extends Actor {

  var quoridor = Quoridor()
  var player:PushEnumerator[JsValue] = new PushEnumerator[JsValue]

  def receive = {
    case QJoin() => {
      sender ! QConnected(player)
      send_board()
    }
    case QBoard() => {
      send_board()
    }
    case QMove((x,y)) => {
      quoridor = quoridor.move_player(x,y)
      send_board()
    }
    case QWall(x, y, ori) => {
      quoridor = quoridor.put_wall(x, y, ori)
      send_board()
    }
  }

  def send_board() = {
    val msg = JsObject(
      Seq(
        "size" -> JsNumber(quoridor.size),
        "round" -> JsNumber(quoridor.round),
        "players" -> JsArray(quoridor.players
                                     .map({case (x,y) =>
                                            JsArray(List(JsNumber(x),
                                                         JsNumber(y)))})),
        "walls" -> JsArray(quoridor.walls.toList
                                   .map({case (x, y, ori) =>
                                          JsArray(List(JsNumber(x),
                                                       JsNumber(y),
                                                       JsBoolean(ori)))})),
        "possible_moves" -> JsArray(quoridor.possible_moves.toList
                                            .map({case (x, y) =>
                                                   JsArray(List(JsNumber(x),
                                                                JsNumber(y)))}))
      )
    )
    player.push(msg)
  }
 
}

case class QConnected(enumerator:Enumerator[JsValue])
case class QJoin()
case class QBoard()
case class QMove(pos: (Int,Int))
case class QWall(x: Int, y: Int, ori:QOrientation)
