package com.symbol.uisample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.IOException;

public class NotificationSkipReceiver extends BroadcastReceiver {

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    public void onReceive(Context context, Intent intent) {

        settings = context.getSharedPreferences(ListenForHeadphones.PREFS_NAME, 0);
        editor = settings.edit();

        editor.putString("musicState","skip song");
        editor.commit();
    }
}
