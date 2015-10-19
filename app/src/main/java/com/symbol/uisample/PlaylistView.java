package com.symbol.uisample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class PlaylistView extends Activity{

    private ListView mainListView;
    private Button addSong;
    private Button deletePlaylist;
    private Button done;

    private ArrayAdapter<String> listAdapter;//displays song titles
    private StringBuilder sb = new StringBuilder();//used to write paths to file
    private StringBuilder sbPlaylist = new StringBuilder();//stores returned playlists (after deleting this)
    private ArrayList<String> songPaths = new ArrayList<String>();//holds paths of all songs in playlist
    private String playlistName;
    private String playlistFileName = "";
    private int playlistPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_layout);
        Intent i = getIntent();
        playlistPosition = i.getIntExtra("playlist-index", 0);
        ArrayList<String> temp = StaticMethods.readFile("playlist_names.txt",getBaseContext());
        playlistName = temp.get(playlistPosition);
        playlistFileName = playlistName+".txt";
        mainListView = (ListView) findViewById( R.id.mainListView );
        addSong = (Button) findViewById(R.id.add_song);
        deletePlaylist = (Button) findViewById(R.id.delete_playlist);
        done = (Button) findViewById(R.id.set_playlist);

        populateListView();

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(), parent.getItemAtPosition(position).toString() + " has been deleted.", Toast.LENGTH_LONG).show();
                deleteSongFromPlaylist(position, parent.getItemAtPosition(position).toString());
            }
        });

        deletePlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    StaticMethods.write(playlistFileName, "", getBaseContext());
                    ArrayList<String> temp = StaticMethods.readFile("playlist_names.txt", getBaseContext());
                    temp.remove(playlistPosition);
                    for (String s : temp) {
                        sbPlaylist.append(s + "\n");
                    }
                    StaticMethods.write("playlist_names.txt", sbPlaylist.toString(), getBaseContext());
                    //if deleting current playlist, switch mode to shuffle
                    String currentPlaylistFileName = StaticMethods.readFirstLine("setPlaylist.txt",getBaseContext());
                    if(playlistFileName.equals(currentPlaylistFileName)){
                        StaticMethods.write("options.txt","0",getBaseContext());
                        Toast.makeText(getBaseContext(),"Shuffle mode is set", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                }
                Intent i = new Intent(getBaseContext(), MainActivity.class);
                startActivity(i);
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //must have at least one song before "creating" playlist
                if(songPaths == null || songPaths.size() == 0){
                    Toast.makeText(getBaseContext(),"Playlists must have at least 1 song in them",Toast.LENGTH_LONG).show();
                }else{
                    Intent i = new Intent(getBaseContext(),MainActivity.class);
                    startActivity(i);
                }
            }
        });

        addSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(),AlbumsListView.class);
                i.putExtra("playlist_file_name",playlistFileName);
                startActivity(i);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        populateListView();
    }


    private void deleteSongFromPlaylist(int index, String title){
        songPaths = StaticMethods.readFile(playlistFileName,getBaseContext());
        songPaths.remove(index);
        sb = new StringBuilder();
        for(String s: songPaths){
            sb.append(s + "\n");
        }
        try{
            StaticMethods.write(playlistFileName,sb.toString(),getBaseContext());
        }catch(IOException e){}
        listAdapter.remove(title);
    }

    private void populateListView(){
        songPaths = StaticMethods.readFile(playlistFileName,getBaseContext());

        if(songPaths != null && songPaths.size() > 0){
            for(String s: songPaths){
                sb.append(s + "\n");
            }
        }

        listAdapter = new ArrayAdapter<String>(this, R.layout.example_row);
        if(songPaths != null && songPaths.size() > 0){
            for(String s: songPaths){
                String s2 = StaticMethods.getTitleFromUriString(s);
                listAdapter.add(s2);
            }
        }
        mainListView.setAdapter( listAdapter );
    }

}
