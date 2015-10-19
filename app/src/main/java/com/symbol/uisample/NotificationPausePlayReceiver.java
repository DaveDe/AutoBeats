package com.symbol.uisample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

public class NotificationPausePlayReceiver extends BroadcastReceiver {

    private String musicState = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            musicState = StaticMethods.readFirstLine("musicState.txt",context);
        }catch(IOException e){}
        if(musicState.equals("play")){
            try{
                StaticMethods.write("musicState.txt","pause",context);
            }catch(IOException e){}
        }else if(musicState.equals("pause")){
            try{
                StaticMethods.write("musicState.txt","play",context);
            }catch(IOException e){}
        }
    }
}