window.Application ||= {}
window.Application.launch_quoridor = (ws_url) ->
  q =
    id: "" 
    turn: false
    size: 9
    round: 0
    winner: -1
    players: [[[4,0],9], [[4,8],9]]
    walls: []
    possible_moves: []
  if (`'WebSocket' in window`)
    chatSocket = new WebSocket(ws_url)
  else if (`'MozWebSocket' in window`)
    chatSocket = new MozWebSocket(ws_url)
  else
    return
  chatSocket.onmessage = (event) ->
    q = JSON.parse(event.data)
    if !q.turn
      q.possible_moves = []
    draw_board()
  chatSocket.onopen = (e) ->
    chatSocket.send(JSON.stringify({type: "board"}))
  
  # init
  Ori =
    H: true
    V: false
  
  Dir =
    N: [ 0, 1]
    S: [ 0,-1]
    W: [-1, 0]
    E: [ 1, 0]
  
  # graphics dimensions
  x_shift     = 30
  y_shift     = 160
  case_size   = 30
  wall_size   = 10
  pawn_size   = 30
  p_shift     = case_size/2
  x_hud_shift = x_shift + 30
  y_hud_shift = y_shift - 100
  players_color = ["#00F", "#F00"]
  dir_buttons = [[[    case_size+wall_size, 2*(case_size+wall_size)], Dir.N],
                 [[    case_size+wall_size,                       0], Dir.S],
                 [[                      0,     case_size+wall_size], Dir.W],
                 [[2*(case_size+wall_size),     case_size+wall_size], Dir.E]]
                .map(([[x,y], dir]) -> [[x_hud_shift+x, y_hud_shift+y+40], dir])
  gwalls = {}
  for x in [0..q.size-2]
    for y in [0..q.size-2]
      gwalls[[x, y, Ori.H]] = [[x, y, Ori.H],
                               [x_shift+x*(case_size+wall_size),
                                y_shift+y*(case_size+wall_size)+case_size]]
      gwalls[[x, y, Ori.V]] = [[x, y, Ori.V],
                               [x_shift+x*(case_size+wall_size)+case_size,
                                y_shift+y*(case_size+wall_size)]]
  gcases = {}
  for x in [0..q.size-1]
    for y in [0..q.size-1]
      gcases[[x,y]] = [x_shift + x*(case_size+wall_size),
                       y_shift + y*(case_size+wall_size)]
  big_wall = {}
  big_wall[Ori.H] = [case_size*2+wall_size, wall_size]
  big_wall[Ori.V] = [wall_size, case_size*2+wall_size]
  small_wall = {}
  small_wall[Ori.H] = [case_size, wall_size]
  small_wall[Ori.V] = [wall_size, case_size]
  pwalls_shift = [[x_shift-case_size+wall_size,
                   y_shift-(case_size*2+wall_size+10),
                   wall_size*2],
                  [x_shift+q.size*(case_size+wall_size),
                   y_shift-(case_size*2+wall_size+10),
                   -wall_size*2]]  

  # graphics objects
  board   = null
  bd_ctx = null
  buffer = null
  br_ctx = null
  b_x_offset = 0
  b_y_offset = 0

  find_pos = (obj) ->
    curleft = curtop = 0
    if (obj.offsetParent)
      loop
        curleft += obj.offsetLeft
        curtop += obj.offsetTop
        break unless (obj = obj.offsetParent)
    return [curleft,curtop]

  init = ->
    #board = document.getElementById('board')
    $('#board>*').remove()
    board = document.createElement('canvas')
    board.setAttribute("width", "500")
    board.setAttribute("height", "550")
    $('#board').append($(board))

    # compute board offset
    [b_x_offset, b_y_offset] = find_pos(board)
    
    # init all things
    bd_ctx = board.getContext('2d')
    buffer = document.createElement('canvas')
    buffer.width  = board.width
    buffer.height = board.height
    br_ctx = buffer.getContext('2d')
    board.addEventListener('mousedown', ev_mousedown, false)
    board.addEventListener('mousemove', ev_mousemove, false) 
    draw_board()
  
  fill_circle = (ctx, x, y, s) ->
    ctx.beginPath()
    ctx.arc(x, y, s, 0, 2*Math.PI, false)
    ctx.closePath()
    ctx.fill()
  
  draw_board = -> 
    br_ctx.clearRect(-100, -200, buffer.width*2+100, buffer.height*2+200)
    
    # print hud
    br_ctx.fillStyle = "#000"
    br_ctx.font = "20px sans-serif"
    br_ctx.fillText("Id: " + q.id,
                    x_hud_shift, y_hud_shift-35)
    br_ctx.fillText("You: ",
                    x_hud_shift+150, y_hud_shift-35)
    if q.winner>=0
        msg = if q.winner==q.player then "Winner" else "Looser"
        br_ctx.fillText(msg, x_hud_shift+250, y_hud_shift-35)

    br_ctx.fillText("Round: "+q.round,
                    x_hud_shift, y_hud_shift)
    br_ctx.fillText("Turn: ",
                    x_hud_shift+150, y_hud_shift)
    # You player
    br_ctx.fillStyle = players_color[q.player%q.players.length]
    fill_circle(br_ctx,
                x_hud_shift+225,
                y_hud_shift-40,
                pawn_size/2)
    # Turn player
    br_ctx.fillStyle = players_color[q.round%q.players.length]
    fill_circle(br_ctx,
                x_hud_shift+225,
                y_hud_shift-5,
                pawn_size/2)
  
    # print board 
    br_ctx.fillStyle = "#BBB"
    for x in [0..q.size-1]
      for y in [0..q.size-1]
        br_ctx.fillRect(x_shift + x*(case_size+wall_size),
                        y_shift + y*(case_size+wall_size),
  		        case_size, case_size)

    # print possibles moves
    br_ctx.fillStyle = "#888"
    for [x,y] in q.possible_moves
      br_ctx.fillRect(x_shift + x*(case_size+wall_size),
                      y_shift + y*(case_size+wall_size),
    		      case_size, case_size)

    # print walls
    br_ctx.fillStyle = "#000"
    for w in q.walls
      [[kx, ky, kori], [dx,dy]] = gwalls[w]
      [sx, sy] = big_wall[kori]
      br_ctx.fillRect(dx, dy, sx, sy)

    # print players
    for i in [0..q.players.length-1]
      br_ctx.fillStyle = players_color[i]
      [[x,y], nbwalls] = q.players[i]
      fill_circle(br_ctx,x_shift+x*(case_size+wall_size)+p_shift,y_shift+y*(case_size+wall_size)+p_shift,pawn_size/2)
      # print remaining walls 
      [dx, dy, vx] = pwalls_shift[i]
      [sx, sy] = big_wall[Ori.V]
      for w in [1..nbwalls]
        br_ctx.fillRect(dx+w*vx, dy, sx, sy)
  
    bd_ctx.clearRect(0, 0, board.width, board.height)
    bd_ctx.drawImage(buffer, 0, 0)
  
  ev_mousemove = (ev) ->
    if (ev.layerX || ev.layerX==0) # Firefox
      x = ev.layerX - b_x_offset
      y = ev.layerY - b_y_offset
    else if (ev.offsetX || ev.offsetX==0) # Opera
      x = ev.offsetX
      y = ev.offsetY
  
    if q.turn
      todraw = []
      bd_ctx.fillStyle = "#FFF"
      for _, v of gwalls
        [[kx, ky, kori], [dx,dy]] = v
        [sx,sy] = small_wall[kori]
        [tx,ty] = big_wall[kori]
        bd_ctx.fillRect(dx, dy, tx, ty)
        if (dx<x && x<dx+sx && dy<y && y<dy+sy)
          todraw[todraw.length] = [dx, dy, tx, ty]
      # print walls
      bd_ctx.fillStyle = "#000"
      for w in q.walls
        [[kx, ky, kori], [dx,dy]] = gwalls[w]
        [sx, sy] = big_wall[kori]
        bd_ctx.fillRect(dx, dy, sx, sy)
      bd_ctx.fillStyle = "#AAA"
      for [dx, dy, tx, ty] in todraw
        bd_ctx.fillRect(dx, dy, tx, ty)

  ev_mousedown = (ev) ->
    if (ev.layerX || ev.layerX==0) # Firefox
      x = ev.layerX - b_x_offset
      y = ev.layerY - b_y_offset
    else if (ev.offsetX || ev.offsetX==0) # Opera
      x = ev.offsetX
      y = ev.offsetY
    if q.turn
      # check move
      for [dx,dy] in q.possible_moves
        [gx, gy] = gcases[[dx,dy]]
        if (gx<x && x<gx+case_size && gy<y && y<gy+case_size)
          chatSocket.send(JSON.stringify({type: "move", x: dx, y: dy}))

      # check walls
      for _, v of gwalls
        [[kx, ky, kori], [dx,dy]] = v
        [sx,sy] = small_wall[kori]
        if (dx<x && x<dx+sx && dy<y && y<dy+sy)
          chatSocket.send(JSON.stringify({type: "wall", x: kx, y: ky, ori: kori}))
    
  init()
