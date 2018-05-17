package com.rtc.cast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.emitter.Emitter;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private Socket mSocket;

    {
        try {
            mSocket = IO.socket("http://192.168.1.4:8889"); // Mifi device
            // mSocket = IO.socket("http://192.168.1.3:8889"); // outdated Mifi device
            //mSocket = IO.socket("http://192.168.1.5:8889"); // home wifi (how
        } catch (URISyntaxException e) {
            Toast.makeText(MainActivity.this, "Failed to connect!! " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse Sdp : " + e.getMessage());
            }
            Log.d(TAG, "offer SDP : " + sdp);
        }
    };

    private View.OnClickListener mConnectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            JSONObject data = new JSONObject();
            try {
                data.put("msg", "Android Client");
            } catch (JSONException e) {

            }
            mSocket.emit("onwebpeerconnected", data);
        }
    };

    private static final String TAG = MainActivity.class.getSimpleName();
    private VideoRenderer.Callbacks remoteRender;
    private PeerConnection client;
    private Button mConnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConnectButton = (Button) findViewById(R.id.btn_connect);
        mConnectButton.setOnClickListener(mConnectButtonClickListener);
        mSocket.connect();
        mSocket.on("offer", mOnSdpOffer);
    }

    private void onRemoteOffer(String sdp) {
        Log.d(TAG, "On offer");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("new message", mOnNewMessage);
    }
}
