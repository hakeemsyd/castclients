package com.rtc.cast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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
            mSocket = IO.socket("http://192.168.1.3:8889/");
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

    private static final String TAG = MainActivity.class.getSimpleName();
    private VideoRenderer.Callbacks remoteRender;
    private PeerConnection client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSocket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("new message", mOnNewMessage);
    }
}
