package models

import scala.collection.immutable._

// Orientation of the walls
object QOrientation extends Enumeration {
  type QOrientation = Boolean
  val Horizontal = true
  val Vertical   = false

  def rev(ori:QOrientation):QOrientation = ori match {
    case Horizontal => Vertical
    case Vertical   => Horizontal
  }

  def coor(ori:QOrientation):(Int,Int) = ori match {
    case Horizontal => (1,0)
    case Vertical   => (0,1)
  }
}
import QOrientation._

// Direction to move
object QDirection extends Enumeration {
  type QDirection = (Int, Int)
  val North = ( 0, 1)
  val East  = ( 1, 0)
  val South = ( 0,-1)
  val West  = (-1, 0)

  def clockwise(dir:QDirection):QDirection = {
    val (x,y) = dir; (y, -x)
  }

  def counterclockwise(dir:QDirection):QDirection = {
    val (x,y) = dir; (-y, x)
  }

  def inv(dir:QDirection):QDirection = { 
    val (x,y) = dir; (-x, -y)
  }
}
import QDirection._

// Game board Companion
object Quoridor{
  val goals = List(South, North, East, West)

  def apply(size:Int = 9) = {
    new Quoridor(size, HashSet(), 0, List((size/2, 0), (size/2,size-1)), None)
  }

  def apply(size:Int,
            walls:HashSet[(Int,Int,QOrientation)],
            round:Int,
            players:List[(Int,Int)]) = {
    new Quoridor(size, walls, round, players, None)
  }

  def apply(size:Int,
            walls:HashSet[(Int,Int,QOrientation)],
            round:Int,
            players:List[(Int,Int)],
            jump:Option[(Int,Int)]) = {
    new Quoridor(size, walls, round, players, jump)
  }

}

// Game board class
class Quoridor(val size:Int,
               val walls:HashSet[(Int,Int,QOrientation)],
               val round:Int,
               val players:List[(Int,Int)],
               val jump:Option[(Int,Int)]) {
  
  val turn = round % players.length

  // check a move (return the same position if impossible)
  def possible_move(pos:(Int,Int), dir:QDirection): (Int,Int) = {
    val (x,y) = pos
    val (xs,ys) = dir
    val posm@(xm,ym) = (x+xs, y+ys)
    // bloc reversed jump
    jump match {
      case Some(pos) if posm==pos => return pos
      case whatever => {}
    }
    if (xm<0 | xm>=size | ym<0 | ym>=size) return pos
    dir match {
      case North if walls.contains((  x,  y,Horizontal)) |
                    walls.contains((x-1,  y,Horizontal)) => pos
      case South if walls.contains((  x,y-1,Horizontal)) |
                    walls.contains((x-1,y-1,Horizontal)) => pos
      case East  if walls.contains((  x,  y,Vertical  )) |
                    walls.contains((  x,y-1,Vertical  )) => pos
      case West  if walls.contains((x-1,  y,Vertical  )) |
                    walls.contains((x-1,y-1,Vertical  )) => pos
      case whatever => posm
    }
  }

  // move current player to the direction if possible
  def move_player(dir:QDirection):Quoridor = {
    val pos = players(turn)
    val npos = possible_move(pos, dir)
    if (pos==npos) this
    else if (players.contains(npos)) { // if a player at this position
      val nnpos = possible_move(npos, dir)
      if (npos==nnpos) { // if impossible to jump in front
        val cwpos = possible_move(npos, QDirection.clockwise(dir))
        val ccwpos = possible_move(npos, QDirection.counterclockwise(dir))
        if (cwpos==npos & ccwpos==npos) // everything blocked
          this 
        else if (cwpos==npos) 
          Quoridor(size, walls, round+1, players.updated(turn, ccwpos))
        else if (ccwpos==npos)
          Quoridor(size, walls, round+1, players.updated(turn, cwpos))
        else // need the player to choice
          Quoridor(size, walls, round, players.updated(turn, npos), Some(pos))
      }else Quoridor(size, walls, round+1, players.updated(turn, nnpos))
    }else Quoridor(size, walls, round+1, players.updated(turn, npos))
  }

  // put a wall if possible
  def put_wall(x:Int, y:Int, ori:QOrientation):Quoridor = {
    // impossible to put a wall if jumping
    if (!jump.isEmpty) return this
    // check board limits
    if (x<0 | x>=size-1 | y<0 | y>=size-1) return this
    val (ox,oy) = QOrientation.coor(ori)
    // check wall collision
    if (walls.contains((x,y,QOrientation.rev(ori)))|
        walls.contains((x,y,ori))                  |
        walls.contains((x+ox,y+oy,ori))            |
        walls.contains((x-ox,y-oy,ori))            ) return this
    val nwall = (x,y,ori)
    val nquoridor = Quoridor(size, walls + nwall, round+1, players)
    // check goals
    if (players.zip(Quoridor.goals)
               .forall({case (player, goal) =>
                         nquoridor.seek_goal(Queue(player),
                                              HashSet(),
                                              goal)}))
       nquoridor
    else this
  }

  // check if a position satify a goal
  def satisfy_goal(pos:(Int,Int), goal:QDirection):Boolean = {
    val (x, y) = pos
    goal==North & y==0      |
    goal==South & y==size-1 |
    goal==West  & x==0      | 
    goal==East & x==size-1
  }

  // check if a goal if reachable
  def seek_goal(todo:Queue[(Int,Int)],
                done:HashSet[(Int,Int)],
                goal:QDirection):Boolean = {
    if (todo.isEmpty) return false
    val (current, ntodo) = todo.dequeue
    val ndone = done + current
    val news = List(North,South,East,West).map(possible_move(current,_))
                                          .filter(x => !(todo.contains(x) |
                                                         ndone.contains(x)))
    if (news.exists(satisfy_goal(_, goal))) true
    else seek_goal(ntodo.enqueue(news), ndone, goal)
  }
}
