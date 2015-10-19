package com.symbol.uisample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ListenForHeadphones extends Service {

    private boolean isRunning = false;
    private MediaPlayer mp = new MediaPlayer();
    private int length = 0;
    private String musicState = "";
    private ArrayList<String> songPaths;
    private int mode = 0;
    private String notificationContent = "";
    private String notificationTitle = "";
    private boolean headphones = false;
    private String seekBarProgress;
    private long elapsedTime;
    private boolean resume = false;

    LocalBroadcastManager songFinished;
    LocalBroadcastManager seekBarUpdate;

    BroadcastReceiver playPauseReceiver;

    static final public String RESULT1 = "SONG_FINISHED_RESULT";
    static final public String MESSAGE1 = "SONG_FINISHED_MESSAGE";
    static final public String RESULT2 = "SEEKBAR_RESULT";
    static final public String MESSAGE2 = "SEEKBAR_MESSAGE";


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "AutoBeats Initialized", Toast.LENGTH_LONG).show();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        LocalBroadcastManager.getInstance(this).registerReceiver((playPauseReceiver), new IntentFilter(HomeFragment.RESULT3));
        try{//initialize files
            StaticMethods.write("musicState.txt", "pause", getBaseContext());
            StaticMethods.write("nextSong.txt", "none", getBaseContext());
            StaticMethods.write("seekbar.txt","-",getBaseContext());
            StaticMethods.write("songduration.txt","10",getBaseContext());
            StaticMethods.write("currentSongUri.txt","",getBaseContext());
            StaticMethods.write("speaker-status.txt","no",getBaseContext());
            StaticMethods.write("import_status.txt", "no", getBaseContext());
        }catch(IOException e){}
        songPaths = StaticMethods.getSongPath(getBaseContext());

        return START_STICKY;
    }
    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        IntentFilter bfilter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter bfilter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter bfilter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(receiver, filter);
        registerReceiver(bReceiver, bfilter1);
        registerReceiver(bReceiver, bfilter2);
        registerReceiver(bReceiver, bfilter3);
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        ComponentName eventReceiver = new ComponentName(getPackageName(), HeadphoneButtonListener.class.getName());
        am.registerMediaButtonEventReceiver(eventReceiver);//register media button
        songFinished = LocalBroadcastManager.getInstance(this);
        seekBarUpdate = LocalBroadcastManager.getInstance(this);

        playPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(HomeFragment.MESSAGE3);
                if(s.equals("play")&&(!headphones)&&(!isRunning)){
                    new Thread()
                    {
                        public void run() {
                            if(!isRunning){
                                startMusic();
                            }
                        }
                    }.start();
                }
            }
        };
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch(state){
                    case 1:
                        headphones = true;
                        try{Thread.sleep(100);}catch(InterruptedException e){}//ensure startMusic thread has time to stop
                         new Thread()
                            {
                                public void run() {
                                    if(!isRunning){
                                        startMusic();
                                    }
                                }
                            }.start();
                        break;
                    case 0:
                        headphones = false;
                        stopMusic();
                        break;
                }
            }
        }
    };

    private void startMusic(){
        try{
            StaticMethods.write("musicState.txt", "play", getBaseContext());//always plays when headphones are plugged in
        }catch(IOException e){}
        try{
            mode = Integer.parseInt(StaticMethods.readFirstLine("options.txt",getBaseContext()));
        }catch(IOException e){}
        if(mode != 2){
            isRunning = true;
            if(resume){
                mp.start();
                makeNotification(notificationTitle,notificationContent);
                resume = false;
            }else{
                mp.stop();
                mp.release();
                mp = new MediaPlayer();
                playSong();
            }
            //keep playing songs until headphones are unplugged
            while(true){
                length = mp.getCurrentPosition();
                if(!isRunning){
                    mp.pause();
                    length = mp.getCurrentPosition();
                    break;
                }
                try{
                    seekBarProgress = StaticMethods.readFirstLine("seekbar.txt",getBaseContext());
                }catch(IOException e){}
                if(seekBarProgress == null){
                    seekBarProgress = "-";
                }
                if(!seekBarProgress.equals("-")){
                    if(!seekBarProgress.equals("start")){
                        int jumpTo = Integer.parseInt(seekBarProgress);
                        mp.seekTo(jumpTo);
                        try{
                            StaticMethods.write("seekbar.txt","-",getBaseContext());
                        }catch(IOException e){}
                    }
                }else{
                    sendSeekbarUpdate(Integer.toString(length));
                }
                try{
                    musicState = StaticMethods.readFirstLine("musicState.txt",getBaseContext());
                }catch(IOException e){}
                if(musicState == null){
                    musicState = "";
                }
                if(musicState.equals("pause") && mp.isPlaying()){
                    mp.pause();
                    makeNotification(notificationTitle,notificationContent);
                }
                if(musicState.equals("skip song")){
                    onComplete();
                }
                if(musicState.equals("play") && !mp.isPlaying() && (mp.getDuration() <= length + 500)) {//song finished (within 500 milliseconds)
                    onComplete();
                }
                if (musicState.equals("play") && !mp.isPlaying() && (mp.getDuration() > length)) {//resume from pause
                    mp.seekTo(length);
                    mp.start();
                    makeNotification(notificationTitle,notificationContent);
                }
                try{
                    mode = Integer.parseInt(StaticMethods.readFirstLine("options.txt",getBaseContext()));
                }catch(IOException e){}
                if(mode == 2){
                    isRunning = false;
                }
            }
        }
    }

    private void stopMusic(){
        try{
            StaticMethods.write("musicState.txt","pause",getBaseContext());
        }catch(IOException e){}
        isRunning = false;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        elapsedTime = 0;
        resume = true;
        new Thread()
        {
            public void run() {
                while(true){
                    try{
                        Thread.sleep(1000);
                        elapsedTime += 1;
                    }catch(InterruptedException e){}
                    if(elapsedTime >= 300){
                        resume = false;
                        break;
                    }
                    if(headphones){
                        break;
                    }
                }
            }
        }.start();
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                headphones = true;
                stopMusic();
                try{Thread.sleep(100);}catch(InterruptedException e){}
                new Thread()
                {
                    public void run() {
                        if(!isRunning){
                            startMusic();
                        }
                    }
                }.start();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                headphones = false;
                stopMusic();
            }
        }
    };

    private void playSong(){
        int song = 0;
        String nextSong = getNextSong();
        if(!nextSong.equals("none")){
            notificationContent = StaticMethods.getTitleFromUriString(nextSong);
            notificationTitle = StaticMethods.getArtistFromUriString(nextSong);
            try{
                mp.setDataSource(nextSong);
                mp.prepare();
                mp.start();
                isRunning = true;
                try {
                    StaticMethods.write("currentSongUri.txt", nextSong, getBaseContext());
                } catch (IOException e) {}
                int songDuration = mp.getDuration();
                try{
                    StaticMethods.write("songduration.txt",Integer.toString(songDuration),getBaseContext());
                }catch(IOException e){}
                sendSongCompleteResult(notificationTitle+"\n"+notificationContent);
            }catch(IOException e){}
        }else {
            try {
                mode = Integer.parseInt(StaticMethods.readFirstLine("options.txt", getBaseContext()));
            } catch (IOException e) {}
            try {
                if (mode == 0) {
                    Random rand = new Random();
                    song = rand.nextInt(songPaths.size());
                    notificationContent = StaticMethods.getTitleFromUriString(songPaths.get(song));
                    notificationTitle = StaticMethods.getArtistFromUriString(songPaths.get(song));
                    mp.setDataSource(songPaths.get(song));
                    mp.prepare();
                    mp.start();
                    isRunning = true;
                    try {
                        StaticMethods.write("currentSongUri.txt",songPaths.get(song),getBaseContext());
                    } catch (IOException e) {}
                    int songDuration = mp.getDuration();
                    try{
                        StaticMethods.write("songduration.txt",Integer.toString(songDuration),getBaseContext());
                    }catch(IOException e){}
                    sendSongCompleteResult(notificationTitle+"\n"+notificationContent);
                }
                if (mode == 1) {
                    String file = StaticMethods.readFirstLine("setPlaylist.txt", getBaseContext());
                    if(file == null || file.equals("")){
                        Toast.makeText(getBaseContext(),"Please set Playlist",Toast.LENGTH_LONG).show();//POSSIBLE BUG IF PLAYLIST IS NOT SET
                        mp = new MediaPlayer();
                        isRunning = false;
                    }else {
                        ArrayList<String> playListSongs = StaticMethods.readFile(file, getBaseContext());
                        Random rand = new Random();
                        song = rand.nextInt(playListSongs.size());
                        notificationContent = StaticMethods.getTitleFromUriString(playListSongs.get(song));
                        notificationTitle = StaticMethods.getArtistFromUriString(playListSongs.get(song));
                        mp.setDataSource(playListSongs.get(song));
                        mp.prepare();
                        mp.start();
                        isRunning = true;
                        try {
                            StaticMethods.write("currentSongUri.txt", playListSongs.get(song), getBaseContext());
                        } catch (IOException e) {
                        }
                        int songDuration = mp.getDuration();
                        try {
                            StaticMethods.write("songduration.txt", Integer.toString(songDuration), getBaseContext());
                        } catch (IOException e) {
                        }
                        sendSongCompleteResult(notificationTitle+"\n"+notificationContent);
                    }
                }
            } catch (IOException e) {
            }
        }
        makeNotification(notificationTitle, notificationContent);
    }
    private void onComplete(){
        mp.stop();
        mp.release();
        mp = new MediaPlayer();
        playSong();
        try{
            StaticMethods.write("musicState.txt", "play", getBaseContext());//used for skip function
        }catch(IOException e){}
    }

    private void makeNotification(String artist, String title){

        Intent intent1 = new Intent(getBaseContext(),NotificationPausePlayReceiver.class);
        PendingIntent playPauseIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent2 = new Intent(getBaseContext(),NotificationSkipReceiver.class);
        PendingIntent skipIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent2, PendingIntent.FLAG_CANCEL_CURRENT);

        int icon = R.mipmap.icon;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, title+" is playing", when);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setTextViewText(R.id.title, artist);
        contentView.setTextViewText(R.id.text, title);
        contentView.setOnClickPendingIntent(R.id.play_pause, playPauseIntent);
        String temp = "";
        try{
            temp = StaticMethods.readFirstLine("musicState.txt",getBaseContext());
        }catch(IOException e){}
        if(temp.equals("pause")){
            contentView.setImageViewResource(R.id.play_pause,R.mipmap.play);
        }else{
            contentView.setImageViewResource(R.id.play_pause,R.mipmap.pause);
        }
        contentView.setOnClickPendingIntent(R.id.skip,skipIntent);
        notification.contentView = contentView;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = contentIntent;

        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.PRIORITY_HIGH;

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);

    }

    private String getNextSong(){
        String nextSong = "";
        String temp = "";
        try{
            temp = StaticMethods.readFirstLine("nextSong.txt",getBaseContext());
        }catch(IOException e){}
        nextSong = temp;
        try{
            StaticMethods.write("nextSong.txt", "none", getBaseContext());
        }catch(IOException e){}
        return nextSong;
    }

    public void sendSongCompleteResult(String message) {
        Intent intent = new Intent(RESULT1);
        if(message != null)
            intent.putExtra(MESSAGE1, message);
        songFinished.sendBroadcast(intent);
    }

    public void sendSeekbarUpdate(String message) {
        Intent intent = new Intent(RESULT2);
        if(message != null)
            intent.putExtra(MESSAGE2, message);
        seekBarUpdate.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playPauseReceiver);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}