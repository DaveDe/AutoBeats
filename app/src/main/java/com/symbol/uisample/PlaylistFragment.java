package com.symbol.uisample;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlaylistFragment extends Fragment{

    private ListView mainListView;
    private ArrayAdapter<String> listAdapter;//displays playlist names
    private Button addPlaylist;
    private ArrayList<String> playlistNames;
    private StringBuilder sb;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    public static PlaylistFragment newInstance(int page, String title) {
        PlaylistFragment fragmentFirst = new PlaylistFragment();
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences(ListenForHeadphones.PREFS_NAME, 0);
        editor = settings.edit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        mainListView = (ListView) view.findViewById( R.id.mainListView );
        listAdapter = new ArrayAdapter<String>(getActivity(), R.layout.example_row);
        addPlaylist = (Button) view.findViewById(R.id.add_playlist);
        playlistNames = new ArrayList<String>();
        sb = new StringBuilder();

        //import google play playlists, only after starting from destroyed activity
        String importStatus = settings.getString("importStatus","");
        if(!importStatus.equals("imported")){
            findDevicePlaylists();
            editor.putString("importStatus","imported");
            editor.commit();
        }

        playlistNames = StaticMethods.readFile("playlist_names.txt",getActivity().getBaseContext());

        if(playlistNames != null && playlistNames.size() > 0){
            for(String s: playlistNames) {
                listAdapter.add(s);
                sb.append(s+"\n");
            }
        }

        mainListView.setAdapter(listAdapter);

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(view.getContext(), PlaylistView.class);
                i.putExtra("playlist-index", position);//position starts at 0
                startActivity(i);
            }
        });

        addPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.dialog_box);
                dialog.setTitle("Enter Playlist Name:");

                Button button = (Button) dialog.findViewById(R.id.button);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        EditText edit = (EditText) dialog.findViewById(R.id.editText);
                        String text = edit.getText().toString();

                        boolean duplicate = false;

                        //ensure playlist name is unique
                        ArrayList<String> playlists = StaticMethods.readFile("playlist_names.txt",getActivity().getBaseContext());
                        for(String s: playlists){
                            if(s.equals(text)){
                                Toast.makeText(getActivity().getBaseContext(),"Can't have duplicate playlist names", Toast.LENGTH_LONG).show();
                                duplicate = true;
                                dialog.dismiss();
                                //Intent i = new Intent(getActivity().getBaseContext(),MainActivity.class);
                                //startActivity(i);
                            }
                        }
                        if(!duplicate){
                            dialog.dismiss();
                            listAdapter.add(text);
                            try {
                                sb.append(text + "\n");
                                StaticMethods.write("playlist_names.txt", sb.toString(), getActivity().getBaseContext());
                            } catch (IOException e) {}
                            Intent i = new Intent(getActivity().getBaseContext(), AlbumsListView.class);
                            i.putExtra("playlist_file_name",text+".txt");
                            startActivity(i);
                        }
                    }
                });

                dialog.show();
            }
        });
        return view;
    }

    //updates files with google play playlists
    private void findDevicePlaylists(){

        //names correspond to id's with same index
        ArrayList<String> playlistNames = new ArrayList<String>();
        ArrayList<String> playlistID = new ArrayList<String>();

        String[] proj = { "playlist_name","_id"};
        Uri playlistUri = Uri.parse("content://com.google.android.music.MusicContent/playlists");
        Cursor cursor = getActivity().getContentResolver().query(playlistUri, proj, null, null, null);
        if(cursor != null){
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                playlistNames.add(cursor.getString(0));
                playlistID.add(cursor.getString(1));
                cursor.moveToNext();
            }

            for(int i = 0; i < playlistID.size(); i++){

                //titles correspond to paths with same index
                ArrayList<String> songTitle = new ArrayList<String>();
                ArrayList<String> songPaths = new ArrayList<String>();

                String[] proj2 = { MediaStore.Audio.Media.TITLE };
                String playListRef = "content://com.google.android.music.MusicContent/playlists/" + playlistID.get(i) + "/members";
                Uri songUri = Uri.parse(playListRef);
                Cursor songCursor = getActivity().getContentResolver().query(songUri, proj2, null, null, null);

                songCursor.moveToFirst();
                while (!songCursor.isAfterLast()) {
                    songTitle.add(songCursor.getString(0));
                    songCursor.moveToNext();
                }


                for(String s: songTitle){
                    songPaths.add(StaticMethods.getPathFromTitle(s, getActivity().getBaseContext()));
                }

                //update playlist_names.txt (make sure google play playlists are first)
                StringBuilder sb = new StringBuilder();
                for(String s: playlistNames){
                    sb.append(s+"\n");
                }

                ArrayList<String> temp = StaticMethods.readFile("playlist_names.txt",getActivity().getBaseContext());

                //dont add the same playlist twice (google play playlist is already in playlist_names.txt)
                for(String s: temp){
                    boolean add = true;
                    for(String s2: playlistNames){
                        if(s.equals(s2)){
                            add = false;
                        }
                    }
                    if(add){
                        sb.append(s+"\n");
                    }
                }
                try{
                    //StaticMethods.write("playlist_names.txt","",getActivity().getBaseContext());
                    StaticMethods.write("playlist_names.txt",sb.toString(),getActivity().getBaseContext());
                }catch(IOException e){}

                //create new file for each playlist found, populate with song paths
                StringBuilder sb2 = new StringBuilder();
                for(String s: songPaths){
                    sb2.append(s+"\n");
                }
                try{
                    StaticMethods.write(playlistNames.get(i)+".txt",sb2.toString(),getActivity().getBaseContext());
                }catch(IOException e){}

            }

        }

    }

}
