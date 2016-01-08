package com.symbol.uisample;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class StaticMethods {

    public static void write (String filename, String data, Context c) throws IOException{
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(c.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
        }
    }

    public static String readFirstLine (String filename,Context c) throws IOException{
        String ret = "0";
        try {
            InputStream inputStream = c.openFileInput(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(inputStreamReader);
                ret = br.readLine();
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return ret;
    }

    public static ArrayList<String> getSongPath(Context c) {
        ArrayList<String> songPaths = new ArrayList<String>();
        Uri exContent = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " !=0";

        String[] projection = new String[]{
                MediaStore.Audio.Media.DATA
        };
        Cursor cursor = c.getContentResolver().query(exContent, projection, selection, null, MediaStore.Audio.Media.DISPLAY_NAME + " DESC");//table - columns - etc...
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            songPaths.add(cursor.getString(0));
            cursor.moveToNext();
        }
        return songPaths;
    }

    public static String getPathFromMediaUri(Context context,Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Audio.Media.DATA};//
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static ArrayList<String> readFile(String filename, Context c){
        ArrayList<String> ret = new ArrayList<String>();
        String currentLine = "";
        try {
            InputStream inputStream = c.openFileInput(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(inputStreamReader);
                while((currentLine = br.readLine()) != null){
                    ret.add(currentLine);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return ret;
    }

    public static String getTitleFromUriString(String uri){
        String[] titleParts = uri.split("/");
        String[] title = titleParts[titleParts.length-1].split("\\.");
        return title[title.length-2];
    }

    public static String getArtistFromUriString(String uri){
        String[] titleParts = uri.split("/");
        String artist = titleParts[titleParts.length-3];
        return artist;
    }

    public static String getPathFromTitle(String songTitle, Context context){
        String ret = "";

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.TITLE + "=?",
                new String[]{"" + songTitle},
                MediaStore.Audio.Media.TITLE + " ASC");

        c.moveToFirst();
        while (!c.isAfterLast()) {
            ret = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
            c.moveToNext();
        }

        return ret;
    }

    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            imgIn.recycle();
            System.gc();
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            imgIn.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            file.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgIn;
    }
}
