package com.rtc.cast;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

/**
 * Created by hakeemsyd on 5/17/18.
 */

public class ConnectionObserver implements PeerConnection.Observer {

    private final String TAG = ConnectionObserver.class.getSimpleName();
    private final String mName;

    public ConnectionObserver(String name) {
        mName = name;
    }
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, mName + " onSignalingChange, state: " + signalingState.name());
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, mName + " onIceConnectionChange, iceConnectionState: " + iceConnectionState.name());

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, mName + " onIceConnectionReceivingChange, b: " + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, mName + " onIceGatheringChange, iceGatheringState: " + iceGatheringState.name());
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, mName + " onIceCandidate, iceCandidate: " + iceCandidate.sdp);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, mName + " onIceCandidatesRemoved: ");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }
}
