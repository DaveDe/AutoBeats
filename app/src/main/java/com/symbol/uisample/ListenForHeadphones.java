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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ListenForHeadphones extends Service {

    static final public String RESULT1 = "SONG_FINISHED_RESULT";
    static final public String MESSAGE1 = "SONG_FINISHED_MESSAGE";
    static final public String RESULT2 = "SEEKBAR_RESULT";
    static final public String MESSAGE2 = "SEEKBAR_MESSAGE";
    public static final String PREFS_NAME = "Info";

    private boolean isRunning = false;
    private boolean resume = false;
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
    private boolean noMusic;

    private LocalBroadcastManager songFinished;
    private LocalBroadcastManager seekBarUpdate;
    private BroadcastReceiver playPauseReceiver;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
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
                    new Thread(){
                        public void run(){
                            if(!isRunning){
                                startMusic();
                            }
                        }
                    }.start();
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "AutoBeats Initialized", Toast.LENGTH_LONG).show();
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();

        editor.putBoolean("serviceStarted4", true);
        editor.putString("musicState", "pause");
        editor.putString("nextSong","none");
        editor.putString("seekbar", "-");
        editor.putString("songDuration", "10");
        editor.putString("currentSongUri", "");
        editor.putString("speakerStatus", "no");
        editor.putString("importStatus", "no");
        editor.commit();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        LocalBroadcastManager.getInstance(this).registerReceiver((playPauseReceiver), new IntentFilter(HomeFragment.RESULT3));
        songPaths = StaticMethods.getSongPath(getBaseContext());
        noMusic = false;

        return START_STICKY;
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

        editor.putString("musicState","play");
        editor.commit();
        mode = settings.getInt("options",0);

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

                if(noMusic){
                    break;
                }
                length = mp.getCurrentPosition();
                if(!isRunning){
                    mp.pause();
                    length = mp.getCurrentPosition();
                    break;
                }
                seekBarProgress = settings.getString("seekbar","-");
                if(!seekBarProgress.equals("-")){
                    if(!seekBarProgress.equals("start")){
                        int jumpTo = Integer.parseInt(seekBarProgress);
                        mp.seekTo(jumpTo);
                        editor.putString("seekbar","-");
                    }
                }else{
                    sendSeekbarUpdate(Integer.toString(length));
                }
                musicState = settings.getString("musicState","");
                if(musicState.equals("pause") && mp.isPlaying()){
                    mp.pause();
                    makeNotification(notificationTitle, notificationContent);
                }
                if(musicState.equals("skip song")){
                    onComplete();
                }
                if(musicState.equals("prev song")){
                    prevOnComplete();
                }
                if(musicState.equals("play") && !mp.isPlaying() && (mp.getDuration() <= length + 500)) {//song finished (within 500 milliseconds)
                    onComplete();
                }
                if (musicState.equals("play") && !mp.isPlaying() && (mp.getDuration() > length)) {//resume from pause
                    mp.seekTo(length);
                    mp.start();
                    makeNotification(notificationTitle,notificationContent);
                }
                mode = settings.getInt("options",0);
                if(mode == 2){
                    isRunning = false;
                }
                editor.commit();
            }
        }
    }

    private void stopMusic(){

        editor.putString("musicState","pause");
        editor.commit();
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

        if(songPaths.size() == 0){
            noMusic = true;
            ToastThread t = new ToastThread();
            t.execute();
        }else{
            noMusic = false;
            String state = settings.getString("musicState","pause");
            int song = 0;
            String nextSong = getNextSong();
            if(state.equals("prev song")){
                String songUri = "";
                try{
                    songUri = StaticMethods.readFirstLine("songLog.txt",getBaseContext());

                    if(songUri == null || songUri.equals("")){
                    }else{
                        playSongHelper(songUri);
                    }
                    //remove songUri from songLog.txt
                    ArrayList<String> songs = StaticMethods.readFile("songLog.txt",getBaseContext());
                    StringBuilder sb = new StringBuilder();
                    //skip first song (list decreases in size by 1)
                    for(int i = 1; i < songs.size(); i++){
                        sb.append(songs.get(i) + "\n");
                    }
                    StaticMethods.write("songLog.txt", sb.toString(), getBaseContext());

                }catch(IOException e){}
                return;
            }
            if(!nextSong.equals("none")){
                playSongHelper(nextSong);
            }else{
                mode = settings.getInt("options",0);
                if (mode == 0) {
                    Random rand = new Random();
                    song = rand.nextInt(songPaths.size());
                    String songUri = songPaths.get(song);
                    playSongHelper(songUri);
                }
                if (mode == 1) {
                    String file = settings.getString("setPlaylist","");
                    if(file.equals("")){
                        Toast.makeText(getBaseContext(),"Please set Playlist",Toast.LENGTH_LONG).show();//POSSIBLE BUG IF PLAYLIST IS NOT SET
                        mp = new MediaPlayer();
                        isRunning = false;
                    }else {
                        ArrayList<String> playListSongs = StaticMethods.readFile(file, getBaseContext());
                        int playlistSongIndex = settings.getInt("playlistSongIndex",0);
                        String songUri = playListSongs.get(playlistSongIndex);
                        playSongHelper(songUri);
                        if(playlistSongIndex < playListSongs.size() - 1) {
                            playlistSongIndex++;
                        }else{
                            playlistSongIndex = 0;
                        }
                        editor.putInt("playlistSongIndex",playlistSongIndex);
                        editor.commit();
                    }
                }
            }

            makeNotification(notificationTitle, notificationContent);
        }


    }

    private void playSongHelper(String songUri){
        try{
            if(songUri != null && !songUri.equals("")){
                notificationContent = StaticMethods.getTitleFromUriString(songUri);
                notificationTitle = StaticMethods.getArtistFromUriString(songUri);
                mp.setDataSource(songUri);
                mp.prepare();
                mp.start();
                isRunning = true;
                editor.putString("currentSongUri", songUri);
                int songDuration = mp.getDuration();
                editor.putString("songDuration",Integer.toString(songDuration));
                editor.commit();
                sendSongCompleteResult(notificationTitle + "\n" + notificationContent);
            }
        }catch(IOException e){}

    }

    private void onComplete(){
        //save song in songLog for previous button to work
        try {
            String temp = settings.getString("currentSongUri","");
            ArrayList<String> songs = StaticMethods.readFile("songLog.txt", getBaseContext());
            if(!temp.equals("")){

                StringBuilder sb = new StringBuilder();
                sb.append(temp + "\n");
                //most recent songs should be on top of list
                for(int i = 0; i < songs.size(); i++){
                    sb.append(songs.get(i)+"\n");
                }
                //make file that logs all songs played.
                StaticMethods.write("songLog.txt", sb.toString(), getBaseContext());
            }

        } catch (IOException e) {}
        mp.stop();
        mp.release();
        mp = new MediaPlayer();
        playSong();
        editor.putString("musicState","play");
        editor.commit();
    }

    private void prevOnComplete(){
        mp.stop();
        mp.release();
        mp = new MediaPlayer();
        playSong();
        editor.putString("musicState","play");
        editor.commit();
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
        String temp = settings.getString("musicState","");
        if(temp.equals("pause")){
            contentView.setImageViewResource(R.id.play_pause,R.mipmap.n_play);
        }else{
            contentView.setImageViewResource(R.id.play_pause,R.mipmap.n_pause);
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
        String nextSong = settings.getString("nextSong","");
        editor.putString("nextSong","none");
        editor.commit();
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

    class ToastThread extends AsyncTask<Void,Void,Void> {

        protected Void doInBackground(Void... v) {
            Handler handler =  new Handler(getBaseContext().getMainLooper());
            handler.post( new Runnable(){
                public void run(){
                    Toast.makeText(getBaseContext(), "Cant find any music on device",Toast.LENGTH_LONG).show();
                    return;
                }
            });
            return null;
        }

        protected void onProgressUpdate(Void... v) {

        }

        protected void onPostExecute(Void... v) {

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playPauseReceiver);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        editor.putBoolean("serviceStarted4", false);
        editor.commit();
        System.out.println("ONDESTROY");
    }

}