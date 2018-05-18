package com.rtc.cast;

/**
 * Created by hakeemsyd on 5/18/18.
 */

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.List;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import android.content.Context;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {
    //private static final String APP_ID = "DE8535E3"; // fb published
    private static final String APP_ID = "8699A6BC"; // hackreveiver
    // private static final String APP_ID = "EC13D157"; // occast
    // private static final String APP_ID = "4760C631"; // hackreceiver
    // private static final String APP_ID = "4F8B3483"; // stock with this proj

    @Override
    public CastOptions getCastOptions(Context context) {
        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setTargetActivityClassName(MainActivity.class.getName())
                .build();
        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(MainActivity.class.getName())
                .build();

        return new CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID) // chromecast default player
                //.setReceiverApplicationId(APP_ID) //chromecast custom player
                .setCastMediaOptions(mediaOptions)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}