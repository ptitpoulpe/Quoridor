import scala.swing._
import scala.swing.Swing._
import scala.swing.{MainFrame, Panel, SimpleGUIApplication}
import scala.swing.event._
import java.awt.{Color, Dimension}
import scala.collection.immutable._

object QuoridorSwingUI extends SimpleSwingApplication {
  
  // initialisation and configuration
  var q = Quoridor()
  val x_shift = 100
  val y_shift = 100
  val case_size = 40
  val wall_size = 5
  val pawn_size = 30
  val p_shift = (case_size-pawn_size)/2
  val x_hud_shift = x_shift + q.size*(case_size+wall_size) + 40
  val y_hud_shift = x_shift + 40
  val players_color = List(Color.red, Color.green, Color.yellow, Color.blue)
  val dir_buttons = HashMap((case_size+wall_size, 2*(case_size+wall_size)) -> QDirection.North,
                            (case_size+wall_size, 0)                       -> QDirection.South,
                            (0, case_size+wall_size)                       -> QDirection.West,
                            (2*(case_size+wall_size), case_size+wall_size) -> QDirection.East)
                    .map({case ((x,y), dir) => ((x_hud_shift+x, y_hud_shift+y+50), dir)})
  val gwalls = (for (x <- List.range(0,q.size-1); y <- List.range(0,q.size)) yield 
                  List( ((x, y, QOrientation.Horizontal),
                         (x_shift+x*(case_size+wall_size),
                          y_shift+y*(case_size+wall_size)+case_size)),
                        ((x, y, QOrientation.Vertical),
                         (x_shift+x*(case_size+wall_size)+case_size,
                          y_shift+y*(case_size+wall_size))))).flatten.toMap
  val big_wall = HashMap(QOrientation.Horizontal -> (case_size*2+wall_size, wall_size),
                         QOrientation.Vertical   -> (wall_size, case_size*2+wall_size))
  val small_wall = HashMap(QOrientation.Horizontal -> (case_size, wall_size),
                           QOrientation.Vertical   -> (wall_size, case_size))

  // board graphics
  lazy val paint = new Component {
    listenTo(mouse.clicks, mouse.moves, keys)

    // handle mouse 
    reactions += {
      case e: MousePressed  => {val x = e.point.x
                                val y = e.point.y
                                // test directions
                                for (((bx,by), dir) <- dir_buttons) 
                                  if (bx<x & x<bx+case_size & by<y & y<by+case_size)
                                    q = q.move_player(dir)
                                // test walls
                                for (((wx,wy,ori), (bx,by)) <- gwalls) {
                                  val (sx, sy) = small_wall(ori)
                                  if (bx<x & x<bx+sx & by<y & y<by+sy) {
                                    q = q.put_wall(wx, wy ,ori)
                                  }
                                }
                                repaint()
                               }
      case _: FocusLost => repaint()
    }

    // paint boaurd and hud
    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)

      // Print Hud
      g.setColor(Color.black)
      g.drawString("Round: %d".format(q.round),
                   x_hud_shift, y_hud_shift)
      g.setColor(players_color(q.round % q.players.length))
      g.fillOval(x_hud_shift+case_size+wall_size+p_shift,
                 y_hud_shift+case_size+wall_size+p_shift+50, pawn_size, pawn_size)
      g.setColor(Color.lightGray)      
      for ((x,y) <- dir_buttons.keys)
        g.fillRect(x, y, case_size,case_size)

      // print board
      g.setColor(Color.lightGray)
      for (x <- List.range(0, q.size))
         for (y <- List.range(0, q.size))
           g.fillRect(x_shift + x*(case_size+wall_size),
                      y_shift + y*(case_size+wall_size),
                      case_size, case_size)

      // print walls
      g.setColor(Color.black)
      for ((x, y, ori) <- q.walls)
        ori match {
          case QOrientation.Horizontal => g.fillRect(x_shift+x*(case_size+wall_size),
                                                     y_shift+y*(case_size+wall_size)+case_size,
                                                     case_size*2+wall_size,
                                                     wall_size)
          case QOrientation.Vertical   => g.fillRect(x_shift+x*(case_size+wall_size)+case_size,
                                                     y_shift+y*(case_size+wall_size),
                                                     wall_size,
                                                     case_size*2+wall_size)
        }

      // print players
      for (((x,y), color) <- q.players.zip(players_color)) {
        g.setColor(color)
        g.fillOval(x_shift+x*(case_size+wall_size)+p_shift,
                   y_shift+y*(case_size+wall_size)+p_shift,
                   pawn_size, pawn_size)
      }       
    }
  }

  def top = new MainFrame {
    title = "Quoridor"
    size = (250, 250)

    contents = paint
  }
}
