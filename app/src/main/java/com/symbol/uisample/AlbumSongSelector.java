package com.symbol.uisample;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;
//displays songs from selected album in a listview with checkboxes
public class AlbumSongSelector extends ListActivity {

    private Button done;
    private ListView lv;

    private List<String> songPaths = new ArrayList<String>();//listed in order of whats displayed
    private ArrayList<String> selectedSongs = new ArrayList<String>();
    private String albumName;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        Intent i = getIntent();
        albumName = i.getStringExtra("album_name");

        done = (Button) findViewById(R.id.done);
        lv = this.getListView();


        String[] column = { MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE, };

        String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";

        String whereVal[] = { albumName };

        String orderBy = android.provider.MediaStore.Audio.Media.TITLE;

        Cursor cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                column, where, whereVal, orderBy);

        if (cursor.moveToFirst()) {
            do {
                songPaths.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            } while (cursor.moveToNext());
        }



        String[] columns =  new String[] {MediaStore.Audio.Media.DISPLAY_NAME};
        int[] to = new int[]{R.id.songNames};

        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, R.layout.songs, cursor, columns, to);
        this.setListAdapter(mAdapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        done.setText("<");

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SparseBooleanArray sparseBooleanArray = lv.getCheckedItemPositions();//maps ints to booleans
                for (int i = 0; i < lv.getCount(); i++) {
                    if (sparseBooleanArray.get(i)) {
                        selectedSongs.add(songPaths.get(i));
                    }
                }
                Intent intent = new Intent();
                intent.putStringArrayListExtra("added_songs", selectedSongs);
                setResult(RESULT_OK, intent);
                finish();//close activity
            }
        });
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        CheckBox cb = (CheckBox) v.findViewById(R.id.checkSong);
        boolean checkState = cb.isChecked();
        if(!checkState){
            cb.setChecked(true);
        }else{
            cb.setChecked(false);
        }
        //Toast.makeText(this, "Clicked row " + position, Toast.LENGTH_SHORT).show();
    }

    //back does nothing
    @Override
    public void onBackPressed() {}


}