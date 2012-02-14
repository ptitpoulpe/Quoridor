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

case class Player(pos:(Int,Int), nbwalls:Int)

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

  def dirs = Set(North, East, South, West)
}
import QDirection._

// Game board Companion
object Quoridor{
  val goals = List(South, North, East, West)

  def apply(size:Int = 9) = {
    new Quoridor(size, HashSet(), 0, List(Player((size/2,     0), size),
                                          Player((size/2,size-1), size)))
  }

  def apply(size:Int,
            walls:HashSet[(Int,Int,QOrientation)],
            round:Int,
            players:List[Player]) = {
    new Quoridor(size, walls, round, players)
  }

}

// Game board class
class Quoridor(val size:Int,
               val walls:HashSet[(Int,Int,QOrientation)],
               val round:Int,
               val players:List[Player]) {
  
  val turn = round % players.length
  
  val winner = players.zip(Quoridor.goals)
                      .find({case (Player(pos, nbwalls), goal) =>
                               satisfy_goal(pos, goal)}) match {
                 case Some((player, _)) => Some(player)
                 case None              => None }

  val possible_moves = {
    val pos@(x,y) = players(turn).pos
    var poss:Set[(Int,Int)] = Set()
    if (winner.isEmpty) {
      for (dir <- QDirection.dirs) {
        val npos = possible_move(pos, dir)
        if (npos!=pos) {
          if (players.exists(_.pos==npos)) {
            val nnpos = possible_move(npos, dir)
            if (nnpos!=npos)
              poss += nnpos
            else 
              poss ++= List(possible_move(npos, QDirection.counterclockwise(dir)),
                            possible_move(npos, QDirection.clockwise(dir)))
                      .filter(_!=npos)
          } else poss += npos
        }
      }
    }
    poss
  }

  // check a move (return the same position if impossible)
  def possible_move(pos:(Int,Int), dir:QDirection): (Int,Int) = {
    val (x,y) = pos
    val (xs,ys) = dir
    val posm@(xm,ym) = (x+xs, y+ys)
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
  def move_player(pos:(Int,Int)):Quoridor = {
    if (possible_moves.contains(pos))
       Quoridor(size, walls, round+1,
                players.updated(turn, Player(pos, players(turn).nbwalls)))
    else
      this
  }

  // put a wall if possible
  def put_wall(x:Int, y:Int, ori:QOrientation):Quoridor = {
    if (!winner.isEmpty) return this
    // check board limits
    val Player(ppos, pnbwalls) = players(turn)
    if (x<0 | x>=size-1 | y<0 | y>=size-1) return this
    if (pnbwalls<=0) return this
    val (ox,oy) = QOrientation.coor(ori)
    // check wall collision
    if (walls.contains((x,y,QOrientation.rev(ori)))|
        walls.contains((x,y,ori))                  |
        walls.contains((x+ox,y+oy,ori))            |
        walls.contains((x-ox,y-oy,ori))            ) return this
    val nwall = (x,y,ori)
    val nquoridor = Quoridor(size, walls + nwall, round+1,
                             players.updated(turn, Player(ppos, pnbwalls-1)))
    // check goals
    if (players.zip(Quoridor.goals)
               .forall({case (Player(pos, nbwalls), goal) =>
                         nquoridor.seek_goal(Queue(pos),
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
