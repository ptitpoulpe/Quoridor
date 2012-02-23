# http://raphaeljs.com/
# http://www.irunmywebsite.com/raphael/SVGTOHTML_LIVE90.php

window.Application ||= {}
window.Application.launch_khet = (ws_url) ->
  khet = 
    id: ""
    round: ""
    player: ""
    turn: ""
    pawns: []
    beam: []
    killed: []
  if (`'WebSocket' in window`)
    chatSocket = new WebSocket(ws_url)
  else if (`'MozWebSocket' in window`)
    chatSocket = new MozWebSocket(ws_url)
  else
    return
  chatSocket.onmessage = (event) ->
    khet = JSON.parse(event.data)
    draw_board()
  chatSocket.onopen = (e) ->
    chatSocket.send(JSON.stringify({type: "board"}))
  sendMove = (mt, md, x, y) ->
    chatSocket.send(JSON.stringify({type:mt, dir:md, x: x, y: y}))


  # constants
  x_shift = 40
  y_shift = 140
  x_shift_board = 6
  y_shift_board = 10
  wall_board = 5
  case_board = 64
  
  # images
  background = new Image()
  background.src = 'assets/images/khet/board.png'
  arrows = new Image()
  arrows.src = 'assets/images/khet/arrows.png'
  blocs = {}
  blocs_type = ['anubis',
                'pharaoh',
                'pyramid',
                'scarab',
                'sphinx',
                'djed',
                'obelisk',
                'stacked-obelisk']

  # drawing
  board = null
  hud = {}
  pawns = {}
  killed = {}
  beam = null
  arrows = null  

  add_arrow = (r) ->
    r.setStart()
    r.path('M 10.6 7.6 C 10.2 9.2 4 15.3 2.7 14.6 C -0.3 12.8 0.8 3.9 1.2 2.7 C 1.6 1.5 2.5 0.9 3.2 1 C 3.9 1.2 11.2 6 10.6 7.6Z')
     .attr({'fill':'#0d3c07','stroke-width':'1pt','stroke':'none','fill-opacity':'1','stroke-opacity':'1'})
    r.path('M 8.5 7.6 C 8.3 8.8 4.2 13 3.2 12.5 C 2.1 11.9 2.9 9.9 2.2 9.6 C 1.3 6.1 2.6 2.7 3.2 2.8 C 3.7 3 8.9 6.4 8.5 7.6Z')
     .attr({'fill':'#2aaf18','stroke-width':'1pt','stroke':'none','fill-opacity':'1','stroke-opacity':'1'})
    r.path('M 2 9.9 C 2.1 11 2.9 12.8 3.6 12.7 C 4.8 12.4 8.5 8.5 8.6 7.8 C 8.7 7.1 4.9 10.7 3.8 10 C 2.7 9.3 3.3 7.8 2.4 7.7 C 1.5 7.7 1.9 9.5 2 9.9Z')
     .attr({'fill':'#208c11','stroke-width':'1pt','stroke':'none','fill-opacity':'1','stroke-opacity':'1'})
    r.path('M 7.5 6.8 C 7.5 6.8 4.9 4.2 4.3 4 C 3.6 3.9 4 5.1 4 5.1 C 4 5.1 7.2 6.3 7.5 6.8Z')
     .attr({'fill':'#ffffff','stroke-width':'1pt','stroke':'none','fill-opacity':'1','stroke-opacity':'1'})
    res = r.setFinish()
    res.data('set', res)
    return res
  
  add_rotate = (r) ->
    r.setStart()
    r.path('M 14.8 7 C 14.7 5.9 9.8 2 8.6 2.3 C 6.6 2.6 6.9 4.6 6.1 4.9 C 5.3 5 -1.8 5.7 1.4 12.1 C 2 13.3 2.4 13.5 3 13.9 C 3.6 14.2 4.5 14.3 5.3 13.5 C 6 12.6 5.7 11.7 5.4 11 C 4.3 8.8 6.3 8.8 7.3 8.3 C 7.6 9.1 8 10.9 8.6 10.9 C 9.2 10.9 15 9.3 14.8 7Z')
     .attr({'fill':'#0d3c07','stroke':'none','stroke-width':'0','fill-opacity':'1','stroke-opacity':'0'})
    r.path('M 13.9 6.9 C 13.9 6.1 9.7 3 8.8 3.3 C 7.9 3.6 8.3 4.7 7.9 5.3 C -1 5.5 1.3 14 4.2 13 C 5.8 12.5 3.1 10.3 4.4 8.8 C 5.4 7.5 8.3 7.2 8.3 7.5 C 8.4 8.2 8.6 10.1 9.1 10.1 C 9.6 10 14.2 7.9 13.9 6.9Z')
     .attr({'fill':'#2aaf18','stroke':'none','stroke-width':'0','fill-opacity':'1','stroke-opacity':'0'})
    r.path('M 1.8 10.7 C 2.2 8.8 2.7 7.7 3.9 6.9 C 5.3 6 7.5 5.8 7.6 5.7 C 7.7 5.5 5.9 5.9 4.6 6.3 C 2.1 6.9 1.6 9.4 1.8 10.7Z')
     .attr({'fill':'#ffffff','stroke':'none','stroke-width':'0','fill-opacity':'1','stroke-opacity':'0'})
    r.path('M 11.9 6.1 C 11.6 5.8 9.6 4.4 9.1 4.3 C 8.5 4.2 8.9 5.1 8.9 5.1 C 8.9 5.1 11.7 5.8 11.9 6.1Z')
     .attr({'fill':'#ffffff','stroke':'none','stroke-width':'0','fill-opacity':'1','stroke-opacity':'0'})
    r.path('M 12.1 9.4 C 12.1 9.4 14.3 8.6 14.3 8.7 C 14.3 8.7 13.9 8.3 13.9 8.3 L 13.3 8.4 L 12.1 8.8 L 12.1 9.4Z')
     .attr({'fill':'none','stroke-width':'1pt','stroke':'none','fill-opacity':'1','stroke-opacity':'1'})
    r.path('M 13.9 6.9 C 14.2 8.1 9 10.2 9 10.2 C 8.5 9.8 8.2 7.7 7.8 7.4 C 6.9 7.7 4.3 8.3 4.1 9.2 C 4 10 4.7 11.7 4.8 12.1 C 5 12.7 4.4 13.3 3.7 13.1 C 3.1 12.8 3 12.3 3 12.3 C 3 12.3 3.7 12.5 3.8 11.9 C 3.9 11.4 2.5 9.3 3.2 8.4 C 3.9 7.6 6.8 6.4 8.5 6.5 L 9.2 9 C 9.2 9 9.9 9 13.9 6.9Z')
     .attr({'fill':'#1f8b10','stroke':'none','stroke-width':'0','fill-opacity':'0.9899','stroke-opacity':'0'})
    res = r.setFinish()
    res.data('set', res)
    return res

  show_arrows = (x, y) ->
    d = wall_board + case_board
    for e in arrows
      e.forEach((i) ->
        i.data('x', x).data('y', y)
        t = i.data('transform')
        i.transform("#{ t }T#{ x*d },#{ y*d }")
        i.toFront()
      )
      e.show()

  hide_arrows = () ->
    for a in arrows
      a.hide()

  init = (svg) ->
    $('#board>*').remove()

    # hud
    dhud = $(document.createElement('div'))
    dhud.append("<b>Turn:</b>")
    hud['turn'] = $(document.createElement('span'))
    dhud.append(hud['turn'])
    $('#board').append(dhud)

    # board
    dboard = document.createElement('div')
    dboard.setAttribute("width", "500")
    dboard.setAttribute("height", "550")
    $('#board').append($(dboard))
    board = Raphael(dboard, 470, 378)
    board.setViewBox(0,0,700,564, true)
    bkg = board.image('assets/images/khet/board.png', 0, 0, 700, 563)

    for i in [0..(blocs_type.length-1)]
      bt = blocs_type[i]
      for [cb, cs] in [[0, 'r'], [1, 'b']]
        img = board.image("assets/images/khet/#{ bt }-#{ cs }.png", 0, 0, 64, 64)
        img.hide()
        blocs[[cb,i]] = img

    _arrows = [[ add_arrow(board),                    "s2,2,0,0", "mv", [ 1, 0]],
               [ add_arrow(board),            "s2,2,0,0r90,-5,8", "mv", [ 0, 1]],
               [ add_arrow(board),           "s2,2,0,0r180,-5,8", "mv", [-1, 0]],
               [ add_arrow(board),           "s2,2,0,0r-90,-5,8", "mv", [ 0,-1]],
               [ add_arrow(board),      "s2,2,0,0r -45,-5,8t5,0", "mv", [ 1,-1]],
               [ add_arrow(board),      "s2,2,0,0r  45,-5,8t5,0", "mv", [ 1, 1]],
               [ add_arrow(board),      "s2,2,0,0r-135,-5,8t5,0", "mv", [-1,-1]],
               [ add_arrow(board),      "s2,2,0,0r 135,-5,8t5,0", "mv", [-1, 1]],
               [add_rotate(board),  "t-15,-3s1.5,1.3,0,0r25,0,0", "rt",    true],
               [add_rotate(board), "t-15,34s1.5,-1.3,0,0r25,0,0", "rt",   false]]
    arrows = []
    for [e, t, mt, i] in _arrows
      [x,y] = [48,24]
      e.data('transform', "#{ t }T#{ x },#{ y }")
      e.data('moveType', mt)
      e.data( 'moveDir',  i)
      e.data('x', -1)
      e.data('y', -1)
      e.forEach((i) ->
        i.attr({"opacity": .4})
      )
      e.transform("#{ t }T#{ x },#{ y }")
      e.mouseover(() ->
        this.data('set').animate({"opacity":  1}, 500)
      ).mouseout(() ->
        this.data('set').animate({"opacity": .4}, 500)
      ).click( () ->
        sendMove(this.data('moveType'), this.data('moveDir'),
                 this.data('x'), this.data('y'))
        hide_arrows()
      )
      arrows[arrows.length] = e
    
    hide_arrows()
    beam = board.set()
    draw_board()
    
  chain = () ->
    bps = this.data("paths")
    i = this.data("i")
    if (i<bps.length)
      this.data("i": i+1).animate({path:bps[i]}, 200, chain)
    else
      for pid, pawn of killed
        pawn.animate({opacity:0}, 2000, () -> this.hide();beam.hide())

  draw_board = ->
    beam.remove()
    beam.clear()
    hud['turn'].text(" "+khet.turn)

    # killed pawn
    for pid, pawn of killed
      pawn.remove()
      delete killed[pid]
    for pawn in khet.killed
      [c, t, [x, y], d] = pawn
      dx = x_shift_board + (wall_board + case_board) * x
      dy = y_shift_board + (wall_board + case_board) * y
      if pawns[pawn]
        killed[pawn] = pawns[pawn]
        delete pawns[pawn]
      else
        p = blocs[[c,t]].clone()
                        .data('x', x)
                        .data('y', y)
                        .translate(dx, dy)
                        .rotate((d+90))
                        .mouseover(() ->
                          if (khet.turn & khet.player==c)
                            show_arrows(this.data('x'), this.data('y'))
                        )
        killed[pawn] = p

    npawns = {}
    for pawn in khet.pawns
      [c, t, [x, y], d] = pawn
      dx = x_shift_board + (wall_board + case_board) * x
      dy = y_shift_board + (wall_board + case_board) * y
      if pawns[pawn]
        npawns[pawn] = pawns[pawn]
        delete pawns[pawn]
      else
        p = blocs[[c,t]].clone()
                        .data('x', x)
                        .data('y', y)
                        .data('c', c)
                        .translate(dx, dy)
                        .rotate((d+90))
                        .mouseover(() ->
                          if (khet.turn & khet.player==this.data('c'))
                            show_arrows(this.data('x'), this.data('y'))
                        )
        npawns[pawn] = p
    # clean pawns
    for pid, pawn of pawns
      pawn.remove()
      delete pawns[pid]
    pawns = npawns


    beam_path = null
    beam_paths = []
    for [x,y,dx,dy] in khet.beam
      mx = x_shift_board + case_board/2 + (case_board+wall_board)*x
      my = y_shift_board + case_board/2 + (case_board+wall_board)*y
      if !beam_path
        beam_path = "M#{ mx } #{ my }"
      else
        beam_path = beam_path + "L#{ mx } #{ my }"
      beam_paths[beam_paths.length] = beam_path
    if beam_path
      b = board.path("M0 0")
               .attr({"stroke-width": 6, stroke: "#000"})
               .data("i": 0)
               .data("paths": beam_paths)
      b.animate({stroke: "#F00"}, 2, chain)
      beam.push(b)

  init()
