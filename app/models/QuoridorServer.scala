package models

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import play.api.Play.current
import models.QDirection._
import models.QOrientation._

object QuoridorServer {
  var quoridors = Map.empty[String, ActorRef] 

  def create_or_join(username:Option[String], qid:Option[String])
        :Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    // Get the id of the quoridor game
    val nqid = qid match { case Some(sid) => Some(sid)
                           case None      => (1 until 100)
                                             .map(_.toString)
                                             .find(!quoridors.contains(_)) }
    (username, nqid) match {
      case (Some(susername), Some(sid)) => {
        if (!quoridors.contains(sid)) 
          quoridors += (sid -> Akka.system.actorOf(Props(new QuoridorServer(sid))))
        val qs = quoridors(sid)
        (qs ? (QJoin(susername), 1 second)).asPromise.map {
          case QConnected(player) =>
            val iteratee = Iteratee.foreach[JsValue] { event => 
                          (event \ "type").asOpt[String] match {
                           case Some("board") => qs ! QBoard()
                           case Some("move")  => (event \ "x").asOpt[Int].map(x =>
                                                 (event \ "y").asOpt[Int].map(y =>
                                                 qs ! QMove(susername,(x,y))))
                           case Some("wall")  => (event \ "x").asOpt[Int].map(x =>
                                                 (event \ "y").asOpt[Int].map(y =>
                                                 (event \ "ori").asOpt[Boolean]
                                                                .map(ori =>
                                                 qs ! QWall(susername, x, y, ori))))
                           case None          => {}
                         }}
            (iteratee, player)
          case QCannotConnect(error) =>
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

class QuoridorServer(id:String) extends Actor {

  var quoridor = Quoridor()
  var players = Map.empty[String, (Int, PushEnumerator[JsValue])]

  def receive = {
    case QJoin(username) => {
      if (players.size<2 | players.contains(username)) {
        val player = Enumerator.imperative[JsValue]()
        players += (username -> ((if (players.contains(username))
                                    players(username)._1
                                  else players.size, 
                                  player)))
        sender ! QConnected(player)
        send_board()
      } else {
        sender ! QCannotConnect("Too many players")
      }
    }
    case QBoard() => {
      send_board()
    }
    case QMove(username, (x,y)) => players.get(username) match {
      case Some((i, _)) if i==quoridor.turn => 
        quoridor = quoridor.move_player(x,y)
        send_board()
        case whatever => {}
    }
    case QWall(username, x, y, ori) => players.get(username) match {
      case Some((i, _)) if i==quoridor.turn =>
        quoridor = quoridor.put_wall(x, y, ori)
        send_board()
      case whatever => {}
    }
  }

  def send_board() = {
    val msg = JsObject(
      Seq(
        "id" -> JsString(id),
        "size" -> JsNumber(quoridor.size),
        "round" -> JsNumber(quoridor.round),
        "players" -> JsArray(quoridor.players
                                     .map({case Player((x,y), nbwalls) =>
                                            JsArray(List(JsArray(List(JsNumber(x),
                                                                      JsNumber(y))),
                                                         JsNumber(nbwalls)))})),
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
    players.foreach {case (username, (i, player)) => 
                      player.push(msg ++ JsObject(Seq("turn" -> 
                                                      JsBoolean(i==quoridor.turn))))}
  }
 
}

case class QConnected(enumerator:Enumerator[JsValue])
case class QCannotConnect(msg: String)

case class QJoin(username:String)
case class QBoard()
case class QMove(username:String, pos: (Int,Int))
case class QWall(username:String, x: Int, y: Int, ori:QOrientation)
