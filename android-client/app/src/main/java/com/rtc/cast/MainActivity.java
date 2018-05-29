package com.rtc.cast;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

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
import org.webrtc.Logging;


public class MainActivity extends AppCompatActivity {

    private Socket mSocket;

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

    private AdapterView.OnItemSelectedListener mServerUrlSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                disconnect();
                mCurrentUrl = parent.getItemAtPosition(position).toString();
                connect();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            disconnect();
            connect();
        }
    };

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean HW_ACCELERATION_ENABLED = true;
    private PeerConnection mPeerConnection;
    private PeerConnectionFactory mPeerConnectionFactory;
    private String mRemoteSdp;
    private Button mConnectButton;
    private EglBase mRootEglBase;
    private TextView mConnectionStatus;
    private org.webrtc.SurfaceViewRenderer mRemoteVideoView;
    private Spinner mServerUrlsSpinner;
    private List<PeerConnection.IceServer> mRemoteIceServers;
    private String mCurrentUrl;
    private MenuItem mediaRouteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRemoteIceServers = new ArrayList<>();
        initViews();
        initVideos();
        initRtc();
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
        // TODO: Figure out how to enable logging, the following crashes.
        // Logging.enableLogToDebugOutput(Logging.Severity.LS_WARNING);
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
        mConnectionStatus = (TextView) findViewById(R.id.status_text);

        mServerUrlsSpinner = (Spinner) findViewById(R.id.servers_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.server_urls, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerUrlsSpinner.setAdapter(adapter);
        mServerUrlsSpinner.setOnItemSelectedListener(mServerUrlSelectedListener);
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
        rtcConfig.enableDtlsSrtp = false;
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
    protected void onResume() {
        super.onResume();
        connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
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

    private void connect() {
        try {
            String url = mCurrentUrl == null ? getString(R.string.default_server) : mCurrentUrl;
            mSocket = IO.socket(url);
            mSocket.connect();
            mSocket.on("offer", mOnSdpOffer);
            mSocket.on("setice", mOnRemoteIceCandidate);
            setStatusConnected(url);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Exception on connection: " + e.getMessage());
            setStatusDisconnected();
        }
    }

    private void disconnect() {
        setStatusDisconnected();

        if(mSocket!= null) {
            mSocket.disconnect();
            mSocket = null;
        }

        if(mPeerConnection != null) {
            mPeerConnection.close();
            mPeerConnection = null;
        }
    }

    private void setStatusConnected(String url) {
        mConnectionStatus.setText("Connected to : " + url);
        mConnectionStatus.setBackgroundColor(getResources().getColor(R.color.green));
    }

    private void setStatusDisconnected() {
        mConnectionStatus.setText("Disconnected!");
        mConnectionStatus.setBackgroundColor(getResources().getColor(R.color.red));
    }
}
