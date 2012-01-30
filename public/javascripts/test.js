(function() {
  var Dir, Ori, bd_ctx, big_wall, board, br_ctx, buffer, case_size, chatSocket, dir_buttons, draw_board, ev_mousedown, fill_circle, gwalls, init, p_shift, pawn_size, players_color, q, small_wall, wall_size, x, x_hud_shift, x_shift, y, y_hud_shift, y_shift, _ref, _ref2;

  chatSocket = new WebSocket("@routes.Application.quoridor().webSocketURL()");

  q = {
    size: 9,
    round: 20,
    players: [[4, 0], [4, 8]]
  };

  Ori = {
    H: [1, 0],
    V: [0, 1]
  };

  Dir = {
    N: [0, 1],
    SO: [0, -1],
    W: [-1, 0],
    E: [1, 0]
  };

  x_shift = 100;

  y_shift = 100;

  case_size = 40;

  wall_size = 5;

  pawn_size = 30;

  p_shift = case_size / 2;

  x_hud_shift = x_shift + q.size * (case_size + wall_size) + 100;

  y_hud_shift = x_shift + 40;

  players_color = ["#00F", "#F00"];

  dir_buttons = [[[case_size + wall_size, 2 * (case_size + wall_size)], Dir.N], [[case_size + wall_size, 0], Dir.S], [[0, case_size + wall_size], Dir.W], [[2 * (case_size + wall_size), case_size + wall_size], Dir.E]].map(function(_arg) {
    var dir, x, y, _ref;
    (_ref = _arg[0], x = _ref[0], y = _ref[1]), dir = _arg[1];
    return [[x_hud_shift + x, y_hud_shift + y + 40], dir];
  });

  gwalls = {};

  for (x = 0, _ref = q.size - 1; 0 <= _ref ? x <= _ref : x >= _ref; 0 <= _ref ? x++ : x--) {
    for (y = 0, _ref2 = q.size - 1; 0 <= _ref2 ? y <= _ref2 : y >= _ref2; 0 <= _ref2 ? y++ : y--) {
      gwalls[[x, y, Ori.H]] = [x_shift + x * (case_size + wall_size), y_shift + y * (case_size + wall_size) + case_size];
      gwalls[[x, y, Ori.V]] = [x_shift + x * (case_size + wall_size) + case_size, y_shift + y * (case_size + wall_size)];
    }
  }

  big_wall = {
    hor: [case_size * 2 + wall_size, wall_size],
    ver: [wall_size, case_size * 2 + wall_size]
  };

  small_wall = {
    hor: [case_size, wall_size],
    ver: [wall_size, case_size]
  };

  board = null;

  bd_ctx = null;

  buffer = null;

  br_ctx = null;

  init = function() {
    board = document.getElementById('board');
    bd_ctx = board.getContext('2d');
    buffer = document.createElement('canvas');
    buffer.width = board.width;
    buffer.height = board.height;
    br_ctx = buffer.getContext('2d');
    board.addEventListener('mousedown', ev_mousedown, false);
    return draw_board();
  };

  fill_circle = function(ctx, x, y, s) {
    ctx.beginPath();
    ctx.arc(x, y, s, 0, 2 * Math.PI, false);
    ctx.closePath();
    return ctx.fill();
  };

  draw_board = function() {
    var dir, i, x, y, _i, _len, _ref3, _ref4, _ref5, _ref6, _ref7, _ref8;
    br_ctx.clearRect(-100, -200, buffer.width * 2 + 100, buffer.height * 2 + 200);
    br_ctx.fillStyle = "#000";
    br_ctx.font = "20px sans-serif";
    br_ctx.fillText("Round: " + q.round, x_hud_shift, y_hud_shift);
    br_ctx.fillStyle = players_color[q.round % q.players.length];
    fill_circle(br_ctx, x_hud_shift + case_size + wall_size + p_shift, y_hud_shift + case_size + wall_size + p_shift + 40, pawn_size / 2);
    br_ctx.fillStyle = "#BBB";
    for (_i = 0, _len = dir_buttons.length; _i < _len; _i++) {
      _ref3 = dir_buttons[_i], (_ref4 = _ref3[0], x = _ref4[0], y = _ref4[1]), dir = _ref3[1];
      br_ctx.fillRect(x, y, case_size, case_size);
    }
    br_ctx.fillStyle = "#BBB";
    for (x = 0, _ref5 = q.size - 1; 0 <= _ref5 ? x <= _ref5 : x >= _ref5; 0 <= _ref5 ? x++ : x--) {
      for (y = 0, _ref6 = q.size - 1; 0 <= _ref6 ? y <= _ref6 : y >= _ref6; 0 <= _ref6 ? y++ : y--) {
        br_ctx.fillRect(x_shift + x * (case_size + wall_size), y_shift + y * (case_size + wall_size), case_size, case_size);
      }
    }
    for (i = 0, _ref7 = q.players.length - 1; 0 <= _ref7 ? i <= _ref7 : i >= _ref7; 0 <= _ref7 ? i++ : i--) {
      br_ctx.fillStyle = players_color[i];
      _ref8 = q.players[i], x = _ref8[0], y = _ref8[1];
      fill_circle(br_ctx, x_shift + x * (case_size + wall_size) + p_shift, y_shift + y * (case_size + wall_size) + p_shift, pawn_size / 2);
    }
    bd_ctx.clearRect(0, 0, board.width, board.height);
    return bd_ctx.drawImage(buffer, 0, 0);
  };

  ev_mousedown = function(ev) {
    return alert(ev.offsetX);
  };

  init();

}).call(this);
