'use strict';

var peerConnection = null;
var answerSent = false;
var offerAnswerOptions = {
  offerToReceiveAudio: 1,
  offerToReceiveVideo: 1
};

// const CHROMECAST_SENDER_URL = "ws://192.168.1.50:8889";
// const CHROMECAST_SENDER_URL = "ws://192.168.1.7:8889"; // Hogwarts
const CHROMECAST_SENDER_URL = "ws://192.168.1.37:8889"; // ATT_Wifi hotspot
var startTime;
var remoteVideo = document.getElementById('remoteVideo');
var server_url = ""; //document.getElementById('server_url_text');
var sessionId = ""; // document.getElementById('sessionId');
var connect_btn = null; // document.getElementById('connect_btn');
var disconnect_btn = null;// document.getElementById('disconnect_btn');
setInterval(function () {
       if (socket != null && socket.readyState == 1) {
            // console.log('keep socket alive request')
            socket.send("ping")
        }
}, 4000);
var socket = null;
connect();

function onIceCandidate(pc, event) {
  if (peerConnection != undefined && event.candidate) {
    console.log("peerConnection has iceservers" + JSON.stringify(event.candidate));
    var res = peerConnection.localDescription
    if (!answerSent) {
      console.log('Answer from peerConnection:\n' + res.sdp);
      socket.send(JSON.stringify({sessionId: sessionId, type: 2, data: res.sdp}));
      // socket.close();
      answerSent = true;
    }

    return;
  }

  console.log(getName(pc) + ' ICE candidate: \n' + (event.candidate ?
      event.candidate.candidate : '(null)'));
}

remoteVideo.addEventListener('loadedmetadata', function() {
  console.log('Remote video videoWidth: ' + this.videoWidth +
    'px,  videoHeight: ' + this.videoHeight + 'px');
});

remoteVideo.addEventListener('progress', function() {
  console.log('Buffered: ' + remoteVideo.buffered.length);
});

remoteVideo.onresize = function() {
  console.log('Remote video size changed to ' +
    remoteVideo.videoWidth + 'x' + remoteVideo.videoHeight);
  // document.getElementById('page_title').style.visibility = 'hidden';
  // We'll use the first onsize callback as an indication that video has started
  // playing out.
  if (startTime) {
    var elapsedTime = window.performance.now() - startTime;
    console.log('Setup time: ' + elapsedTime.toFixed(3) + 'ms');
    startTime = null;
  }
};

function getName(pc) {
  return (pc == peerConnection) ? 'LocalPeer' : 'RemotePeer';
}

function gotStream(stream) {
  console.log('Received local stream');
  remoteVideo.srcObject = stream;
  remoteStream = stream;
  callButton.disabled = false;
}

function onCreateSessionDescriptionError(error) {
  console.log('Failed to create session description: ' + error.toString());
}

function hanleOfferFromRemote(desc) {
  console.log('LS offer\n' + desc.sdp);
  var servers = null;
  var options = {
    optional: [
        {DtlsSrtpKeyAgreement: false}
    ]
  }

  peerConnection = new RTCPeerConnection(servers, options);
  console.log('Created remote peer connection object peerConnection');
  peerConnection.onicecandidate = function(e) {
    onIceCandidate(peerConnection, e);
  };
  peerConnection.oniceconnectionstatechange = function(e) {
    onIceStateChange(peerConnection, e);
  };
  peerConnection.ontrack = gotRemoteStream;

  peerConnection.setRemoteDescription(desc).then(
    function() {
      onSetRemoteSuccess(peerConnection);
    },
    onSetSessionDescriptionError
  );
  console.log('Static answer set');
  peerConnection.createAnswer().then(
    onCreateAnswerSuccess,
    onCreateSessionDescriptionError
  );
}

function onSetLocalSuccess(pc) {
  console.log(getName(pc) + ' setLocalDescription complete');
}

function onSetRemoteSuccess(pc) {
  console.log(getName(pc) + ' setRemoteDescription complete');
}

function onSetSessionDescriptionError(error) {
  console.log('Failed to set session description: ' + error.toString());
}

function gotRemoteStream(e) {
  console.log('gotstream');
  if (remoteVideo.srcObject !== e.streams[0]) {
    remoteVideo.srcObject = e.streams[0];
    console.log('peerConnection received remote stream');
  }
}

function onCreateAnswerSuccess(desc) {
  console.log('peerConnection setLocalDescription start');
  peerConnection.setLocalDescription(desc).then(
    function() {
      onSetLocalSuccess(peerConnection);
    },
    onSetSessionDescriptionError
  );
}

function onAddIceCandidateSuccess(pc) {
  console.log(getName(pc) + ' addIceCandidate success');
}

function onAddIceCandidateError(pc, error) {
  console.log(getName(pc) + ' failed to add ICE Candidate: ' + error.toString());
}

function onIceStateChange(pc, event) {
    if(pc == null || pc == undefined) {
      return;
    }

    console.log(getName(pc) + ' ICE state: ' + pc.iceConnectionState);
}

function connect() {
  reset();
  var url = CHROMECAST_SENDER_URL;

  console.log('reconnecting to ' + url);
  if (socket != null) {
    setDisconnectedStatus();
    // socket.close();
  }
  socket = new WebSocket(url);

  socket.onopen = function(event) {
    setConnectedStatus(url);
    var startMsg = {
      sessionId: sessionId,
      type: 0,
      data: 'Web Client',
    };
    socket.send(JSON.stringify(startMsg));
  };

  socket.onmessage = function (event) {
    console.log(event.data);
    var msg = JSON.parse(event.data);

    switch(msg.type) {
      case 0:
        break;
      case 1:
          hanleOfferFromRemote({sdp: msg.data, type: 'offer'});
        break;
      case 2:
        break;
      default:
        break;
    }
  }

  socket.onclose = function(event) {
    setDisconnectedStatus();
  }
}

/*connect_btn.onclick = function(event) {
  connect();
}

disconnect_btn.onclick = function(event) {
  reset();
}*/

function setConnectedStatus(url) {
  console.log('Connected to socket at: ' + url);
  /*var s = document.getElementById("status_msg");
  s.innerHTML = 'Connected : ' + url;
  s.style.color = 'green';*/
}

function setDisconnectedStatus() {
  console.log('Socket disconnected');
  /*var s = document.getElementById("status_msg");
  s.innerHTML = 'Disconnected';
  s.style.color = 'red';*/
}

function reset() {
  console.log('Reset state !');
  if (socket != null && socket.readyState == 1) {
    socket.send(JSON.stringify({sessionId: sessionId, type: 3, data: ""}));
  }

  if (peerConnection != null) {
    peerConnection.close();
  }
  peerConnection = null;
  answerSent = false;
  // document.getElementById('page_title').style.visibility = 'visible';
}
