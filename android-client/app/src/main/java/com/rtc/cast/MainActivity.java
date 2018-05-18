package com.rtc.cast;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.framework.CastButtonFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {

    private Socket mSocket;

    {
        try {
            mSocket = IO.socket("http://192.168.1.4:8889"); // Mifi device
            // mSocket = IO.socket("http://192.168.1.3:8889"); // outdated Mifi device
            //mSocket = IO.socket("http://192.168.1.5:8889"); // home wifi (how
        } catch (URISyntaxException e) {
            Log.e(TAG, "Exception on connection: " + e.getMessage());
        }
    }

    private Emitter.Listener mOnNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "mOnNewMessage");
        }
    };

    private Emitter.Listener mOnSdpOffer = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            String sdp = "";
            try {
                sdp = data.getString("sdp");
                mRemoteSdp = sdp;
                Log.d(TAG, "offer SDP : " + sdp);
                onRemoteOffer(sdp);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse Sdp : " + e.getMessage());

            }
        }
    };

    private Emitter.Listener mOnRemoteIceCandidate = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (args == null) {
                return;
            }

            String str = (String) args[0];
            JSONObject json = null;
            try {
                json = new JSONObject(str);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to create ice json");
            }

            if (json != null) {
                onIceCandidateReceived(json);
            }
        }
    };

    private View.OnClickListener mConnectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            JSONObject data = new JSONObject();
            try {
                data.put("msg", "Android Client");
            } catch (JSONException e) {
                Log.d(TAG, "Failed to parse json");
            }
            mSocket.emit("onwebpeerconnected", data);
        }
    };

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean HW_ACCELERATION_ENABLED = true;
    private PeerConnection mPeerConnection;
    private PeerConnectionFactory mPeerConnectionFactory;
    private String mRemoteSdp;
    private Button mConnectButton;
    private EglBase mRootEglBase;
    private org.webrtc.SurfaceViewRenderer mRemoteVideoView;
    private List<PeerConnection.IceServer> mRemoteIceServers;
    private MenuItem mediaRouteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRemoteIceServers = new ArrayList<>();
        initViews();
        initVideos();
        initRtc();
        mSocket.connect();
        mSocket.on("offer", mOnSdpOffer);
        mSocket.on("setice", mOnRemoteIceCandidate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
        return true;
    }

    private void initRtc() {

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(HW_ACCELERATION_ENABLED)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                mRootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(mRootEglBase.getEglBaseContext());

        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();
        mPeerConnectionFactory = builder
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void initViews() {
        mRemoteVideoView = (SurfaceViewRenderer) findViewById(R.id.remote_gl_surface_view);
        mConnectButton = (Button) findViewById(R.id.btn_connect);
        mConnectButton.setOnClickListener(mConnectButtonClickListener);
    }


    private void initVideos() {
        mRootEglBase = EglBase.create();
        mRemoteVideoView.init(mRootEglBase.getEglBaseContext(), null);
        mRemoteVideoView.setZOrderMediaOverlay(true);
    }

    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(mRemoteIceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfig, new ConnectionObserver("LocalPeerConnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                sendAnswer();
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                super.onSignalingChange(signalingState);
                mPeerConnection.createAnswer(new CustomSdpObserver("createAnswer") {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        super.onCreateSuccess(sessionDescription);
                        mPeerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);
                    }
                }, new MediaConstraints());
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.toString());
                super.onAddStream(mediaStream);
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });


        mPeerConnection.setRemoteDescription(new CustomSdpObserver("setRemoteDescription"), new SessionDescription(SessionDescription.Type.OFFER, mRemoteSdp));
    }


    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        final AudioTrack audioTrack = stream.audioTracks.get(0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "gotStream: " + stream.toString());
                try {
                    mRemoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addSink(mRemoteVideoView);
                    // force volume
                    audioTrack.setVolume(10);
                    audioTrack.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void onRemoteOffer(String sdp) {
        Log.d(TAG, "On offer");
        runOnUiThread(() -> {
            createPeerConnection();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mPeerConnection.close();
        mPeerConnection = null;
        mSocket.off("new message", mOnNewMessage);
    }

    private void sendAnswer() {
        JSONObject answer = new JSONObject();
        String sdp = mPeerConnection.getLocalDescription().description;
        try {
            answer.put("sdp", sdp);
            Log.d(TAG, "sending Answer to peer: " + sdp);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to put answer in JSON: " + e.getMessage());
        }
        mSocket.emit("answer", answer);
    }


    public void onIceCandidateReceived(JSONObject data) {
        if (mPeerConnection == null) {
            return;
        }

        try {
            mPeerConnection.addIceCandidate(new IceCandidate(data.getString("sdpMid"), data.getInt("sdpMLineIndex"), data.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
