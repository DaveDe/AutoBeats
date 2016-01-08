package com.symbol.uisample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationPausePlayReceiver extends BroadcastReceiver {

    private String musicState = "";

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    @Override
    public void onReceive(Context context, Intent intent) {

        settings = context.getSharedPreferences(ListenForHeadphones.PREFS_NAME, 0);
        editor = settings.edit();

        musicState = settings.getString("musicState","");
        if(musicState.equals("play")){
            editor.putString("musicState","pause");
        }else if(musicState.equals("pause")){
            editor.putString("musicState","play");
        }
        editor.commit();
    }
}