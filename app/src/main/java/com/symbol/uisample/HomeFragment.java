package com.symbol.uisample;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {

    private Bitmap b;
    private BroadcastReceiver songCompleteReceiver;
    private BroadcastReceiver seekBarReceiver;
    private Display display;
    private Point point;
    private SeekBar sb;
    private Button nextSong;
    private Button setMode;
    private ImageButton playPause;
    private ImageButton skip;
    private TextView currentPoint;
    private TextView endPoint;
    private TextView songInfoText;
    private TextView currentMode;

    private int width;
    private int height;
    private int songDuration = 0;
    private String songInfo = "not found";

    LocalBroadcastManager play_pause;

    static final public String RESULT3 = "PLAY_PAUSE_RESULT";
    static final public String MESSAGE3 = "PLAY_PAUSE_MESSAGE";

    // newInstance constructor for creating fragment with arguments
    public static HomeFragment newInstance(int page, String title) {
        HomeFragment fragmentFirst = new HomeFragment();
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstRun();
        display = getActivity().getWindowManager().getDefaultDisplay();
        point = new Point();
        display.getSize(point);
        width = point.x;
        height = point.y;
        songCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                songInfo = intent.getStringExtra(ListenForHeadphones.MESSAGE1);
                changeArtwork();
                try{
                    songDuration = Integer.parseInt(StaticMethods.readFirstLine("songduration.txt",getActivity().getBaseContext()));
                }catch(IOException e){}
                sb.setMax(songDuration);
                String endString = millisecondsToTimeFormat(songDuration);
                endPoint.setText(endString);
            }
        };
        seekBarReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(ListenForHeadphones.MESSAGE2);
                int progress = Integer.parseInt(s);
                sb.setProgress(progress);
            }
        };

        play_pause = LocalBroadcastManager.getInstance(getActivity().getBaseContext());
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        sb = (SeekBar) view.findViewById(R.id.seekBar);
        nextSong = (Button) view.findViewById(R.id.nextSong);
        setMode = (Button) view.findViewById(R.id.setMode);
        playPause = (ImageButton) view.findViewById(R.id.play_pause);
        skip = (ImageButton) view.findViewById(R.id.skip);
        currentPoint = (TextView) view.findViewById(R.id.currentPoint);
        endPoint = (TextView) view.findViewById(R.id.endPoint);
        songInfoText = (TextView) view.findViewById(R.id.song_info);
        currentMode = (TextView) view.findViewById(R.id.currentMode);
        //initialize playPause icon
        playPause.setImageResource(R.mipmap.play);
        //initialize art
        b = BitmapFactory.decodeResource(getResources(), R.mipmap.unknown_album);
        Bitmap mb = StaticMethods.convertToMutable(b);
        Bitmap artwork = mb.createScaledBitmap(mb, width, height / 2, false);
        //iv.setImageBitmap(artwork);
        RelativeLayout relative = (RelativeLayout) getActivity().findViewById(R.id.relative);
        Drawable dr = new BitmapDrawable(artwork);
        if(relative != null){
            relative.setBackgroundDrawable(dr);
        }

        try{
            songDuration = Integer.parseInt(StaticMethods.readFirstLine("songduration.txt",getActivity().getBaseContext()));
        }catch(IOException e){}
        sb.setMax(songDuration);
        String endString = millisecondsToTimeFormat(songDuration);
        endPoint.setText(endString);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                try {
                    StaticMethods.write("seekbar.txt", "start", getActivity().getBaseContext());
                } catch (IOException e) {
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;
                String progressString = millisecondsToTimeFormat(progress);
                currentPoint.setText(progressString);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    StaticMethods.write("seekbar.txt", Integer.toString(progress), getActivity().getBaseContext());
                } catch (IOException e) {
                }
            }
        });

        changeArtwork();

        nextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String musicState = "";
                try {
                    musicState = StaticMethods.readFirstLine("musicState.txt", getActivity().getBaseContext());
                } catch (IOException e) {
                }
                if (musicState.equals("play")) {
                    try {
                        StaticMethods.write("musicState.txt", "pause", getActivity().getBaseContext());
                    } catch (IOException e) {
                    }
                    playPause.setImageResource(R.mipmap.play);
                } else if(musicState.equals("pause")){
                    try {
                        StaticMethods.write("musicState.txt", "play", getActivity().getBaseContext());
                    } catch (IOException e) {
                    }
                    playPause.setImageResource(R.mipmap.pause);
                    sendPlayPauseResult("play");//if headphones are not plugged in, play through speakers
                }
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String state = StaticMethods.readFirstLine("musicState.txt",getActivity().getBaseContext());
                    if(state.equals("pause")){
                        playPause.setImageResource(R.mipmap.pause);
                    }
                    StaticMethods.write("musicState.txt", "skip song", getActivity().getBaseContext());
                } catch (IOException e) {
                }

            }
        });
        setMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.set_mode);
                dialog.setTitle("Choose mode");

                ListView lv = (ListView) dialog.findViewById(R.id.listView);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getBaseContext(),R.layout.example_row);
                adapter.add("Shuffle");
                ArrayList<String> playlists = StaticMethods.readFile("playlist_names.txt",getActivity().getBaseContext());
                if(playlists != null && playlists.size() > 0){
                    adapter.add("Playlist");
                }
                adapter.add("Disable");
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch(position){
                            case 0:
                                currentMode.setText("Shuffle");
                                break;
                            case 1:
                                currentMode.setText("selected playlist name here");
                                break;
                            case 2:
                                currentMode.setText("Disabled");
                        }
                        try {
                            StaticMethods.write("options.txt", Integer.toString(position), getActivity().getBaseContext());
                        } catch (IOException e) {
                        }
                        dialog.dismiss();
                        if(position == 1){
                            createPlaylistDialog();
                        }
                    }
                });
                dialog.show();
            }
        });
        displayMode();
        return view;
    }

    private void displayMode(){
         try{
            String mode = StaticMethods.readFirstLine("options.txt",getActivity().getBaseContext());
            int temp = Integer.parseInt(mode);
            switch(temp){
                case 0:
                    mode = "Shuffle";
                    break;
                case 1:
                    mode = "Playlist";
                    break;
                case 2:
                    mode = "Disable";
                    break;
            }
            currentMode.setText("Current Mode: "+mode);
        }catch(IOException e){}
    }

    private void createPlaylistDialog(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.set_mode);
        dialog.setTitle("Select Playlist");

        ListView lv = (ListView) dialog.findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getBaseContext(),R.layout.example_row);
        ArrayList<String> playlists = StaticMethods.readFile("playlist_names.txt",getActivity().getBaseContext());
        for(String s: playlists){
            adapter.add(s);
        }
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String playlistFileName = adapter.getItem(position)+".txt";
                try{
                    StaticMethods.write("setPlaylist.txt",playlistFileName,getActivity().getBaseContext());
                }catch(IOException e){}
                currentMode.setText("Playlist: "+adapter.getItem(position).toString());
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri path =  data.getData();
            String realPath = StaticMethods.getPathFromMediaUri(getActivity().getBaseContext(), path);
            String title = StaticMethods.getTitleFromUriString(realPath);
            try {
                StaticMethods.write("nextSong.txt", realPath, getActivity().getBaseContext());
            } catch (IOException e) {}
            Toast.makeText(getActivity().getBaseContext(), title+" is next", Toast.LENGTH_LONG).show();
        }else{
            try {
                StaticMethods.write("nextSong.txt", "none", getActivity().getBaseContext());
            } catch (IOException e) {}
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((songCompleteReceiver), new IntentFilter(ListenForHeadphones.RESULT1));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((seekBarReceiver), new IntentFilter(ListenForHeadphones.RESULT2));
    }

    @Override
    public void onResume() {
        super.onResume();

        songCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                songInfo = intent.getStringExtra(ListenForHeadphones.MESSAGE1);
                changeArtwork();
                try{
                    songDuration = Integer.parseInt(StaticMethods.readFirstLine("songduration.txt",getActivity().getBaseContext()));
                }catch(IOException e){}
                sb.setMax(songDuration);
                String endString = millisecondsToTimeFormat(songDuration);
                endPoint.setText(endString);
            }
        };
        seekBarReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(ListenForHeadphones.MESSAGE2);
                int progress = Integer.parseInt(s);
                sb.setProgress(progress);
            }
        };

        changeArtwork();
        //set appropriate play/pause icon
        String musicState = "";
        try {
            musicState = StaticMethods.readFirstLine("musicState.txt", getActivity().getBaseContext());
        } catch (IOException e) {
        }
        if (musicState.equals("play")) {
            playPause.setImageResource(R.mipmap.pause);
        } else if(musicState.equals("pause")){
            playPause.setImageResource(R.mipmap.play);
        }
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(songCompleteReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(seekBarReceiver);
        super.onStop();
    }

    private void changeArtwork(){
        String songPath = "";
        try {
            songPath = StaticMethods.readFirstLine("currentSongUri.txt", getActivity().getBaseContext());
        } catch (IOException e) {
        }
        if (songPath == null || songPath.equals("")) {
            b = BitmapFactory.decodeResource(getResources(), R.mipmap.unknown_album);
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            if (Build.VERSION.SDK_INT >= 14) {
                if(songPath != null && !songPath.equals("")){
                    mmr.setDataSource(songPath, new HashMap<String, String>());
                }
            } else {
                mmr.setDataSource(songPath);
            }
            byte[] data = mmr.getEmbeddedPicture();
            if (data == null || data.length == 0) {
                b = BitmapFactory.decodeResource(getResources(), R.mipmap.unknown_album);
            } else {
                b = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            Bitmap mb = StaticMethods.convertToMutable(b);
            Bitmap artwork = mb.createScaledBitmap(mb, width, height / 2, false);
            //iv.setImageBitmap(artwork);
            RelativeLayout relative = (RelativeLayout) getActivity().findViewById(R.id.relative);
            Drawable dr = new BitmapDrawable(artwork);
            if(relative != null){
                relative.setBackgroundDrawable(dr);
            }
            songInfoText.setText(songInfo);
        }
    }

    private String millisecondsToTimeFormat(int progress){
        String format = "";
        int seconds = progress/1000;
        int minutes = seconds/60;
        if(seconds > 59){
            seconds -= (minutes*60);
        }
        if(seconds > 9){
            format = Integer.toString(minutes)+":"+Integer.toString(seconds);
        }else{
            format = Integer.toString(minutes)+":0"+Integer.toString(seconds);
        }
        return format;
    }

    public void sendPlayPauseResult(String message) {
        Intent intent = new Intent(RESULT3);
        if(message != null)
            intent.putExtra(MESSAGE3, message);
        play_pause.sendBroadcast(intent);
    }

    public void checkFirstRun() {
        boolean isFirstRun = getActivity().getSharedPreferences("FIRSTRUN", Activity.MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun){
            //add dialog here
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.first_time_dialog);
            dialog.setTitle("First Time Setup");

            dialog.show();

            getActivity().getSharedPreferences("FIRSTRUN", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
        }

    }

}