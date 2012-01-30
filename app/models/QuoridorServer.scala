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
     Logger("quoridor").info("done")
     (event \ "type").asOpt[String] match {
       case Some("move") => (event \ "dir").asOpt[List[Int]].map(x =>
                               qs ! QMove((x(0),x(1))))
       case Some("wall") => (event \ "x").asOpt[Int].map(x =>
                            (event \ "y").asOpt[Int].map(y =>
                            (event \ "ori").asOpt[Boolean].map(ori =>
                               qs ! QWall(x, y, ori))))
       case None         => {}
     }
     //qs ! QMove(South)
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
      player.push(JsObject(Seq("test" -> JsString("toto"))))
    }
    case QMove(dir) => {
      quoridor = quoridor.move_player(dir)
      Logger("quoridor").info("send")
      Logger("quoridor").info(quoridor.players.toString())
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
                                                       JsBoolean(ori)))}))
      )
    )
    player.push(msg)
  }
 
}

case class QConnected(enumerator:Enumerator[JsValue])
case class QJoin()
case class QMove(dir: QDirection)
case class QWall(x: Int, y: Int, ori:QOrientation)
