'use strict';

var peerConnection = null;
var answerSent = false;
var offerAnswerOptions = {
  offerToReceiveVideo: 1
};


var startTime;
var remoteVideo = document.getElementById('remoteVideo');
var sessionId = "";
var server_url = "";
var prevConnectionState = "";

var messageBus = null;

init();

function init() {
  cast.receiver.logger.setLevelValue(cast.receiver.LoggerLevel.DEBUG);
  window.mediaElement = document.getElementById('remoteVideo');
  window.mediaManager = new cast.receiver.MediaManager(window.mediaElement);
  window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();
  setupMessageBus();
  window.castReceiverManager.start();
  const context = cast.framework.CastReceiverContext.getInstance();
  const playerManager = context.getPlayerManager();

  mediaManager.onLoad = function (event) {
    /*var metadata = event.data.media.metadata;
    var url = metadata.signalServerUrl === null || metadata.signalServerUrl === undefined ? event.data.media.contentId : metadata.signalServerUrl;
    var sid = metadata.sessionId === null || metadata.sessionId === undefined ? "" : metadata.sessionId;
    console.log('onLoad: connecting with url: ' + url + ', sessionId: ' + sid);
    // connect(url, sid);
    sessionId = sid;*/
    // messageBus.send(sid, JSON.stringify("Hello from chromecast, for twilight"));
  };

  window.castReceiverManager.onSenderDisconnected = function(event) {
    console.log(event);
  if(window.castReceiverManager.getSenders().length == 0 &&
    event.reason == cast.receiver.system.DisconnectReason.REQUESTED_BY_SENDER) {
      reset();
      window.close();
    }
  }
}

function setupMessageBus() {
  // create a CastMessageBus to handle messages for a custom namespace
  messageBus =
    castReceiverManager.getCastMessageBus(
     'urn:x-cast:com.oculus.twilight');
    messageBus.onMessage = function(event) {
    console.log(event.data);
    var msg = JSON.parse(event.data);
      switch(msg.type) {
        case 0:
          break;
        case 1:
            sessionId = msg.sessionId;
            hanleOfferFromRemote({sdp: msg.data, type: 'offer'});
          break;
        case 2:
          break;
        default:
          break;
    }
  }
}

// connect();

function onIceCandidate(pc, event) {
  if (peerConnection != undefined && event.candidate) {
    console.log("peerConnection has iceservers" + JSON.stringify(event.candidate));
    var res = peerConnection.localDescription
    if (!answerSent) {
      console.log('Answer from peerConnection:\n' + res.sdp);
      messageBus.broadcast(JSON.stringify({sessionId: sessionId, type: 2, data: res.sdp}));
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
  //console.log('Buffered: ' + remoteVideo.buffered.length);
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
  if (e.receiver.track.kind == "audio") {
    console.log("ignore audio track");
    return;
  }
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

    if (
      (pc.iceConnectionState == 'closed' ||
        pc.iceConnectionState == 'failed') &&
      prevConnectionState != 'closed' &&
      prevConnectionState != 'failed'
    ) {
      setDisconnectedStatus();
      reset();
    }

    prevConnectionState = pc.iceConnectionState;

    console.log(getName(pc) + ' ICE state: ' + pc.iceConnectionState);
}

function setConnectedStatus(url) {
  // console.log('Connected to socket at: ' + url);
}

function setDisconnectedStatus() {
  console.log('Socket disconnected');
}

function reset() {
  console.log('Reset state !');
  if (peerConnection === null) {
    return;
  }

  if (messageBus) {
    messageBus.broadcast(JSON.stringify({sessionId: sessionId, type: 3, data: ""}));
    messageBus = null;
  }

  peerConnection.close();
  window.castReceiverManager.stop();
  peerConnection = null;
  answerSent = false;
}
