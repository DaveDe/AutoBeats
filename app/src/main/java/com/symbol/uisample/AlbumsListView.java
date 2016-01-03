package com.symbol.uisample;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumsListView extends Activity {

    private ListView mainListView;
    private Button done;

    private ArrayAdapter<String> listAdapter;//displays album names
    private String playlistFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_view);

        Intent i = getIntent();
        playlistFileName = i.getStringExtra("playlist_file_name");

        mainListView = (ListView) findViewById(R.id.mainListView);

        listAdapter = new ArrayAdapter<String>(this, R.layout.example_row);

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = new String[] { MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Artists.Albums.ALBUM,
                MediaStore.Audio.Artists.Albums.ARTIST,
                MediaStore.Audio.Artists.Albums.ALBUM_ART,
                MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS };

        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
        Cursor cursor = this.managedQuery(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder);


        while(cursor.moveToNext()){
            listAdapter.add(cursor.getString(1));
        }
        mainListView.setAdapter( listAdapter );

        done = (Button) findViewById(R.id.done);

        done.setText("<");

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(),MainActivity.class);
                startActivity(i);
            }
        });

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String albumClicked = parent.getItemAtPosition(position).toString();
                //intent to some form of PlaylistCreator, which shows songs in album, with a checkbox
                Intent i = new Intent(getBaseContext(),AlbumSongSelector.class);
                i.putExtra("album_name",albumClicked);
                startActivityForResult(i,1);
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> currentSongs = StaticMethods.readFile(playlistFileName,getBaseContext());
            for(String s: currentSongs){
                sb.append(s+"\n");
            }
            ArrayList<String> added_songs = intent.getStringArrayListExtra("added_songs");
            if (added_songs != null && added_songs.size() > 0) {

                for (String s : added_songs) {
                    sb.append(s + "\n");
                }
                try {
                    StaticMethods.write(playlistFileName, sb.toString(), getBaseContext());
                } catch (IOException e) {
                }
            }
        }
    }

}
