var express = require('express');
var app = express();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var PeerConnection = require('rtcpeerconnection');
var port = process.env.PORT || 8889;

var response = null;
var sessionId = null;
var connectionId = 0;

io.on('connection', function(sock){

  sock.on('answer', function(data) {
    if (response != null) {
      connectionId = Math.floor(Math.random() * 2000000);
      sessionId = 'session_id_' + new Date().valueOf();
      console.log('Answer from web peer, sending back to remote peers');
      var answer = {answer_sdp: data.sdp, rtc_connection_id: connectionId.toString(), rtc_session_id: 'blahABaBlah'}
      response.send(answer);
      response = null;
    }
  });

  sock.on('onwebpeerconnected', function(data) {
    console.log('Peer connected - ' + data.msg);
  });
});

/*app.get('/*', function (req, res) {
  console.log(req);
  io.sockets.emit('req', {
    reqheader : req.headers,
    requrl : req.protocol + "://" + req.headers.host + req.url,
    reqmethod : req.method
  })
});*/

app.post('/rtc/connections', function(req, res) {
  console.log('Serving @' + req.path + ', type: ' + req.query.type );
  if (req.query.type == 'join') {
    // don't respond back yet we will long poll.
    response = res;
    io.sockets.emit('offer', {sdp: req.query.offer_sdp});
  } else if (req.query.type == 'iceserver') {
    if (req.query.candidate != undefined || req.query.candidate != null) {
      io.sockets.emit('setice', req.query.candidate);
      res.send('OK');
    }
  }
});

server.listen(port, function(){
  console.log('Server listening at port %d', port);
});
