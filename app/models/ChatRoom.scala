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
import scala.util.matching.Regex

object ChatRoom {
  
  val RQuoridor = new Regex("""/quoridor *(\S*)""")
  val RKhet     = new Regex("""/khet *(\S*)""") 
  implicit val timeout = Timeout(1 second)
  
  lazy val default =  Akka.system.actorOf(Props[ChatRoom])

  def join(username:String):Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    Logger.info("try to log: "+username)
    (default ? Join(username)).asPromise.map {
      
      case Connected(enumerator) => 
      
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          default ! Talk(username, (event \ "text").as[String])
        }.mapDone { _ =>
          default ! Quit(username)
        }

        (iteratee,enumerator)
        
      case CannotConnect(error) => 
      
        // Connection error

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue,Unit]((),Input.EOF)

        // Send an error and close the socket
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))
        
        (iteratee,enumerator)
       
    }

  }
  
}

class ChatRoom extends Actor {
  
  var members = Map.empty[String, PushEnumerator[JsValue]]
  
  def receive = {
    
    case Join(username) => {
      // Create an Enumerator to write to this socket
      val channel = Enumerator.imperative[JsValue](onStart = self ! NotifyJoin(username))
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + (username -> channel)
        
        sender ! Connected(channel)
      }
    }

    case NotifyJoin(username) => {
      notifyAll("join", username, "has entered the room")
    }
    
    case Talk(username, text) => text match {
        case ChatRoom.RQuoridor(qid) => members(username).push(JsObject(Seq(
                              "kind"    -> JsString("command"),
                              "command" -> JsString("quoridor"),
                              "game_url"  -> JsString(controllers
                                                      .routes
                                                      .Application
                                                      .quoridor(Some(username),
                                                                if (qid=="") None
                                                                else Some(qid)).url))))

        case ChatRoom.RKhet(kid) => members(username).push(JsObject(Seq(
                              "kind"    -> JsString("command"),
                              "command" -> JsString("keth"),
                              "game_url"  -> JsString(controllers
                                                      .routes
                                                      .Application
                                                      .khet(Some(username),
                                                            if (kid=="") None
                                                            else Some(kid)).url))))
        case "/help"     => notifyOne("talk", "", "help message", username)
        case whatever    => notifyAll("talk", username, text)
    }
    
    
    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has leaved the room")
    }
    
  }

  def notifyOne(kind: String, user: String, text: String, dst: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          members.keySet.toList.map(JsString)
        )
      )
    )
    members(dst).push(msg) 
  }

  def notifyAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          members.keySet.toList.map(JsString)
        )
      )
    )
    members.foreach { 
      case (_, channel) => channel.push(msg)
    }
  }
  
}

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class NotifyJoin(username: String)

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)
