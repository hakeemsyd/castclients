package com.rtc.cast;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by hakeemsyd on 5/17/18.
 */

public class CustomSdpObserver implements SdpObserver {
    private static final String TAG = CustomSdpObserver.class.getSimpleName();
    private String mPeerName;

    public CustomSdpObserver(String peerName) {
        mPeerName = peerName;
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {

    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, mPeerName +" SdpObserver: onSetSuccess");

    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(TAG, mPeerName + " SdpObserver: onCreateFailure: " + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(TAG, mPeerName + " SdpObserver: onSetFailure: " + s);
    }
}
