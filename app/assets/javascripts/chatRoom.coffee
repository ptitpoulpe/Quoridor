window.Application ||= {}
window.Application.launch_chatRoom = (ws_url) ->
  if (`'WebSocket' in window`)
    chatSocket = new WebSocket(ws_url)
  else if (`'MozWebSocket' in window`)
    chatSocket = new MozWebSocket(ws_url)
  else
    return
            
  sendMessage = () ->
    chatSocket.send(JSON.stringify({text: $("#talk").val()}))
    $("#talk").val('')
            
  receiveEvent = (event) ->
    data = JSON.parse(event.data)
                
    # Handle errors
    if (data.error)
      chatSocket.close()
      $("#onError span").text(data.error)
      $("#onError").show()
      return
    else
      $("#onChat").show()
    
    if (data.kind=="command")
      $("#board").load(data.game_url)
    else
      # Create the message element
      el = $('<div class="message"><span></span><p></p></div>')
      $("span", el).text(data.user)
      $("p", el).text(data.message)
      $(el).addClass(data.kind)
      if (data.user == '@username')
        $(el).addClass('me')
      $('#messages').append(el)
              
      # Update the members list
      $("#members").html('') 
      $(data.members).each(() -> 
        $("#members").append('<li>' + this + '</li>')
      )
            
  handleReturnKey = (e) ->
    if (e.keyCode == 13)
      e.preventDefault()
      sendMessage()
            
  $("#talk").keypress(handleReturnKey)  
            
  chatSocket.onmessage = receiveEvent

