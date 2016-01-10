package com.symbol.uisample;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {

    static final public String RESULT3 = "PLAY_PAUSE_RESULT";
    static final public String MESSAGE3 = "PLAY_PAUSE_MESSAGE";

    private Bitmap b;
    private BroadcastReceiver songCompleteReceiver;
    private BroadcastReceiver seekBarReceiver;
    private Display display;
    private Point point;
    private SeekBar sb;
    private ImageButton playPause;
    private ImageButton skip;
    private ImageButton prev;
    private TextView currentPoint;
    private TextView endPoint;
    private TextView songInfoText;
    private TextView playlistInfo;
    private RelativeLayout rl;
    private Spinner spinner;

    private int width;
    private int height;
    private int songDuration = 0;
    private String songInfo = "";

    private LocalBroadcastManager play_pause;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    // newInstance constructor for creating fragment with arguments
    public static HomeFragment newInstance(int page, String title) {
        HomeFragment fragmentFirst = new HomeFragment();
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getActivity().getSharedPreferences(ListenForHeadphones.PREFS_NAME, 0);
        editor = settings.edit();

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
                songDuration = Integer.parseInt(settings.getString("songDuration","10"));
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
        playPause = (ImageButton) view.findViewById(R.id.play_pause);
        skip = (ImageButton) view.findViewById(R.id.skip);
        currentPoint = (TextView) view.findViewById(R.id.currentPoint);
        endPoint = (TextView) view.findViewById(R.id.endPoint);
        songInfoText = (TextView) view.findViewById(R.id.song_info);
        playlistInfo = (TextView) view.findViewById(R.id.playlist_info);
        rl = (RelativeLayout) view.findViewById(R.id.relative2);
        prev = (ImageButton) view.findViewById(R.id.prev);
        spinner = (Spinner)view.findViewById(R.id.spinner);

        //initialize playPause icon
        playPause.setImageResource(R.mipmap.play);
        rl.getBackground().setAlpha(200);//out of 255(255 is opaque)
        //initialize art
        b = BitmapFactory.decodeResource(getResources(), R.mipmap.unknown_album);
        Bitmap mb = StaticMethods.convertToMutable(b);
        Bitmap artwork = mb.createScaledBitmap(mb, width, height / 2, false);
        RelativeLayout relative = (RelativeLayout) getActivity().findViewById(R.id.relative);
        Drawable dr = new BitmapDrawable(artwork);
        if(relative != null){
            relative.setBackgroundDrawable(dr);
        }
        songDuration = Integer.parseInt(settings.getString("songDuration","10"));
        sb.setMax(songDuration);
        String endString = millisecondsToTimeFormat(songDuration);
        endPoint.setText(endString);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                editor.putString("seekbar","start");
                editor.commit();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;
                String progressString = millisecondsToTimeFormat(progress);
                currentPoint.setText(progressString);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editor.putString("seekbar",Integer.toString(progress));
                editor.commit();
            }
        });

        changeArtwork();

        String[] paths = {"Shuffle", "Playlist", "Disable"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item,paths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("options", position);
                editor.commit();
                ArrayList<String> playlists = StaticMethods.readFile("playlist_names.txt", getActivity().getBaseContext());
                if (position == 1) {
                    if (playlists.size() == 0) {
                        Toast.makeText(getActivity(), "Swipe left to create playlist", Toast.LENGTH_SHORT).show();
                    } else {
                        createPlaylistDialog();
                    }
                } else {
                    playlistInfo.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getActivity().getBaseContext(), "Nothing Selected", Toast.LENGTH_LONG).show();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String musicState = settings.getString("musicState","");
                if (musicState.equals("play")) {
                    editor.putString("musicState","pause");
                    playPause.setImageResource(R.mipmap.play);
                } else if(musicState.equals("pause")){
                    editor.putString("musicState", "play");
                    playPause.setImageResource(R.mipmap.pause);
                    sendPlayPauseResult("play");//if headphones are not plugged in, play through speakers
                }
                editor.commit();
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state = settings.getString("musicState","");
                if(state.equals("pause")){
                    playPause.setImageResource(R.mipmap.pause);
                }
                editor.putString("musicState","skip song");
                editor.commit();
            }
        });

        prev.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                String state = settings.getString("musicState","");
                if(state.equals("pause")){
                    playPause.setImageResource(R.mipmap.pause);
                }
                editor.putString("musicState", "prev song");
                editor.commit();

            }

        });

        return view;
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
                editor.putString("setPlaylist",playlistFileName);
                editor.putInt("playlistSongIndex",0);
                editor.commit();
                playlistInfo.setText("Playlist: "+adapter.getItem(position).toString());
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
            editor.putString("nextSong",realPath);
            Toast.makeText(getActivity().getBaseContext(), title+" is next", Toast.LENGTH_LONG).show();
        }else{
            editor.putString("nextSong","none");
        }
        editor.commit();
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
                songDuration = Integer.parseInt(settings.getString("songDuration","10"));
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
        String musicState = settings.getString("musicState","");
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
        String songPath = settings.getString("currentSongUri","");
        if (songPath.equals("")) {
            b = BitmapFactory.decodeResource(getResources(), R.mipmap.unknown_album);
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            if (Build.VERSION.SDK_INT >= 14) {
                if(songPath != null && !songPath.equals("")){
                    System.out.println("SongPath: " + songPath);
                    try{
                        mmr.setDataSource(songPath, new HashMap<String, String>());
                    }catch (Exception e){
                        Toast.makeText(getActivity(),"Failed to load artwork",Toast.LENGTH_SHORT).show();
                    }
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

}