package models
import scala.collection.immutable._

object KColor extends Enumeration {
  type KColor = Boolean
  val Red = true
  val Black = false
}
import KColor._

object KDirection extends Enumeration {
  type KDirection = Int
  val North = 90
  val East  = 180
  val South = -90
  val West  = 0

  def dir2pos(dir:KDirection):(Int,Int) = dir match {
    case North => ( 0, -1)
    case  East => ( 1,  0)
    case South => ( 0,  1)
    case  West => (-1,  0)
  }

  def clockwise(dir:KDirection):KDirection = (dir+180)%360-90

  def counterclockwise(dir:KDirection):KDirection = (dir+360)%360-90

  def inv(dir:KDirection):KDirection = (dir+270)%360-90 

  def dirs = Set(North, East, South, West)
}
import KDirection._

object KType extends Enumeration {
  type KType = Int
  val Anubis = 0
  val Pharaoh = 1
  val Pyramid = 2
  val Scarab  = 3
  val Sphinx = 4
  val Djed = 5
  val Obelisk = 6
  val StackedObelisk = 7
}
import KType._

case class Pawn(t:KType, color:KColor, dir:KDirection)

object Khet {

  def apply() = {
    val reds = Map((0,3) -> Pawn(Pyramid, Red,  East),
                   (0,4) -> Pawn(Pyramid, Red, South),
                   (4,0) -> Pawn(StackedObelisk, Red, South),
                   (5,0) -> Pawn(Pharaoh, Red, South),
                   (6,0) -> Pawn(StackedObelisk, Red, South),
                   (7,0) -> Pawn(Pyramid, Red, South),
                   (2,1) -> Pawn(Pyramid, Red,  West),
                   (4,3) -> Pawn(   Djed, Red,  East),
                   (5,3) -> Pawn(   Djed, Red, North),
                   (7,3) -> Pawn(Pyramid, Red, South),
                   (7,4) -> Pawn(Pyramid, Red,  East),
                   (6,5) -> Pawn(Pyramid, Red, South))
    val blacks = reds.map({case ((x, y), Pawn(t, c, d)) => 
                                ((9-x, 7-y), Pawn(t, Black, inv(d)))})
    new Khet(reds ++ blacks, 0)
  }

  def classic() = {
    val reds = Map((0,0) -> Pawn( Sphinx, Red, South),
                   (0,3) -> Pawn(Pyramid, Red,  East),
                   (0,4) -> Pawn(Pyramid, Red, South),
                   (4,0) -> Pawn( Anubis, Red, South),
                   (5,0) -> Pawn(Pharaoh, Red, South),
                   (6,0) -> Pawn( Anubis, Red, South),
                   (7,0) -> Pawn(Pyramid, Red, South),
                   (2,1) -> Pawn(Pyramid, Red,  East),
                   (4,3) -> Pawn( Scarab, Red, North),
                   (5,3) -> Pawn( Scarab, Red,  East),
                   (7,3) -> Pawn(Pyramid, Red, South),
                   (7,4) -> Pawn(Pyramid, Red, South),
                   (6,5) -> Pawn(Pyramid, Red, South))
    val blacks = reds.map({case ((x, y), Pawn(t, c, d)) => 
                                ((9-x, 7-y), Pawn(t, Black, inv(d)))})
    new Khet(reds ++ blacks, 0)
  }

  def apply(pawns:Map[(Int, Int), Pawn], turn:Int) = {
    new Khet(pawns, turn)
  }
}

class Khet(apawns:Map[(Int, Int), Pawn],
           val turn:Int ) {

  val (beam, pawns, kpawns) = beamer(apawns, List(((0,0), South)))
  println("bm"+beam.toString)

  def possible_move(pos:(Int,Int), color:KColor):Boolean = {
    val (x,y) = pos
    color match {
      case Red   if (x==9 | x==1 & (y==0 | y==7)) => false
      case Black if (x==0 | x==8 & (y==0 | y==7)) => false
      case whatever                               => true
    }
  }

  def move(pos:(Int,Int), dir:(Int,Int)):Khet = {
    val ( x,  y) = pos
    val (sx, sy) = dir
    val dpos@(dx, dy) = (x+sx, y+sy)
    if ( 0 > dx | dx > 9 | 0 > dy | dy > 7) return this 
    if (sx < -1 | 1 < sx | sy < -1 | 1 < sy) return this
    pawns.get(pos) match {
      case Some(Pawn(Sphinx,_,_)) => this
      case Some(Pawn(_,c,_)) if !possible_move(dpos, c) => this
      case Some(pawn@Pawn(Scarab,c,d)) =>
        pawns.get((dx, dy)) match {
          case Some(rpawn@Pawn(t,_,_)) if List(Anubis,Pyramid).contains(t) =>
            Khet((pawns - pos - dpos) ++ List((dpos, pawn), (pos, rpawn)), turn+1)
          case None =>
            Khet((pawns - pos) + ((dpos, pawn)), turn+1)
          case whatever => this
        }
      case Some(pawn@Pawn(_,c,d)) =>
        pawns.get((dx, dy)) match {
          case Some(_) => this
          case None    => Khet((pawns - pos) + ((dpos, pawn)), turn+1)
        }
      case None       => this
    }
  }

  def rotate(pos:(Int,Int), cw:Boolean):Khet = {
    val rotate_ = if (cw) clockwise _ else counterclockwise _
    (pawns.get(pos), cw) match {
      case (Some(Pawn(Sphinx, c, d)), cw) if !List((  Red,  East,  true),
                                                  (  Red, South, false),
                                                  (Black,  West,  true),
                                                  (Black, North, false))
                                             .contains((c,d,cw)) => this
      case (Some(pawn@Pawn(t,c,d)), _) =>
        Khet(pawns.updated(pos, Pawn(t,c,rotate_(d))), turn+1)
      case wathever      => this
    }
  }

  def beamer(pawns:Map[(Int, Int), Pawn],
             ray:List[((Int, Int), KDirection)])
            :(List[((Int, Int), KDirection)],
              Map[(Int, Int), Pawn],
              Map[(Int, Int), Pawn])
              = {
    if (turn<2)
      return (List(), pawns, Map.empty[(Int, Int), Pawn])
    val c@(pos@(cx, cy), cd) = ray.last
    if (cx<0 | cx>9 | cy<0 | cy>7)
      return (ray, pawns, Map.empty[(Int, Int), Pawn])
    val (dx, dy) = dir2pos(cd)
    val npos@(nx, ny) = (cx+dx, cy+dy)
    pawns.get(npos) match {
      case Some(Pawn(Pyramid, _, d)) if (d==counterclockwise(cd)) =>
        val nd = counterclockwise(cd)
        beamer(pawns, ray :+ (npos, nd))
      case Some(Pawn(Pyramid, _, d)) if (d==inv(cd)) =>
        val nd = clockwise(cd)
        beamer(pawns, ray :+ (npos, nd))
      case Some(Pawn(Pyramid, _, d)) if (d==inv(cd)) =>
        val nd = clockwise(cd)
        beamer(pawns, ray :+ (npos, nd))
      case Some(Pawn(Djed, _, d)) if (d==cd | d==inv(cd)) =>
        val nd = clockwise(cd)
        beamer(pawns, ray :+ (npos, nd))
      case Some(Pawn(Djed, _, d)) if (d==clockwise(cd) | d==counterclockwise(cd)) =>
        val nd = counterclockwise(cd)
        beamer(pawns, ray :+ (npos, nd))
      case Some(pawn@Pawn(StackedObelisk, c, d)) => 
        (ray :+ (npos, cd),
         pawns.updated(npos, Pawn(Obelisk, c, d)),
         Map(npos -> pawn))
      case Some(pawn@Pawn(_, _, _)) => 
        (ray :+ (npos, cd),
         pawns - npos,
         Map(npos -> pawn))
      case None => 
        beamer(pawns, ray :+ (npos, cd))
    }
  }
  
}
