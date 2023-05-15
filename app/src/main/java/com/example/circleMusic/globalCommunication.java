package com.example.circleMusic;



import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class globalCommunication extends Application {
    private ArrayList<MusicItem> trackList;
    private ArrayList<MusicItem> currentPlayingList;
    private ArrayList<MusicItem> playlistList;
    private int currentPlaying = 0;
    private boolean isFinishedSyncing = false;
    private boolean currentlySyncing = false;
    private boolean isShuffle = false;
    private boolean isPlaylistShuffle = false;
    private boolean isOne = false;
    private int imageId = 0;
    private ArrayList<Drawable> images = new ArrayList<>();
    private boolean isLoaded = false;
    private boolean changedSongAfterFinish = false;
    private boolean isSyncActivated = false;

    public boolean isPlaylistShuffle() {
        return isPlaylistShuffle;
    }

    public void setPlaylistShuffle(boolean playlistShuffle) {
        isPlaylistShuffle = playlistShuffle;
    }

    public void addPlaylist(MusicItem playlist){
        playlistList.add(playlist);
    }

    public ArrayList<MusicItem> getTrackList() {
        return trackList;
    }

    public void setTrackList(ArrayList<MusicItem> musicItemList) {
        this.trackList = musicItemList;
    }

    public ArrayList<MusicItem> getCurrentPlayingList() {
        return currentPlayingList;
    }

    public void setCurrentPlayingList(ArrayList<MusicItem> currentPlayingList) {
        this.currentPlayingList = currentPlayingList;
    }

    public int addImage(Drawable image){
        images.add(image);
        return imageId++;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public Drawable getImage(int id){
        if(id >= 0 && id < images.size())
        {
            return images.get(id);
        }
        return getResources().getDrawable( R.drawable.neonmusicicon );
    }

    public boolean isFinishedSyncing() {
        return isFinishedSyncing;
    }

    public void setFinishedSyncing(boolean finishedSyncing) {
        isFinishedSyncing = finishedSyncing;
    }

    public int getCurrentPlaying() {
        return currentPlaying;
    }

    public void setCurrentPlaying(int currentPlaying) {
        this.currentPlaying = currentPlaying;
    }

    public boolean isCurrentlySyncing() {
        return currentlySyncing;
    }

    public void setCurrentlySyncing(boolean currentlySyncing) {
        this.currentlySyncing = currentlySyncing;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setShuffle(boolean shuffle) {
        isShuffle = shuffle;
    }

    public boolean isOne() {
        return isOne;
    }

    public void setOne(boolean one) {
        isOne = one;
    }

    public int getTrackPositionFromName(MusicItem requestedMusicItem) {
        int counter = -1;
        for(MusicItem musicItem : currentPlayingList)
        {
            counter++;
            if(musicItem.getTitle().equals(requestedMusicItem.getTitle()) && musicItem.getSubTitle().equals(requestedMusicItem.getSubTitle()))
            {
                return counter;
            }
        }
        return 0;
    }


    //Music player

    private MediaPlayer mediaPlayer = new MediaPlayer();


    public void load(String path) throws IOException {
        isLoaded = true;
        mediaPlayer.reset();
        mediaPlayer.setDataSource(path);
        mediaPlayer.setLooping(false);
        mediaPlayer.prepare();
    }

    public void play(){
        mediaPlayer.start();
    }

    public void pause(){
        mediaPlayer.pause();
    }

    public void setProgress(int seconds){
        mediaPlayer.seekTo(seconds * 1000);
    }

    public int getProgress() {return (mediaPlayer.getCurrentPosition()) / 1000; }

    public int getDuration() { return mediaPlayer.getDuration() / 1000; }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isPlaying() { return mediaPlayer.isPlaying(); }

    //Playlists

    public void addToPlaylist(MusicItem musicItem, String playlistName){
        try {
            String line = "";

            String dataToWrite = "";

            File gpxfile = new File( getPlaylistPathFromName(playlistName));

            BufferedReader br = new BufferedReader(new FileReader(gpxfile));
            while ((line = br.readLine()) != null) {
                dataToWrite += line + "\n";
            }
            br.close();

            dataToWrite += musicItem.getTitle().toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "") + ".mp3";


            FileWriter writer = new FileWriter(gpxfile);
            writer.append(dataToWrite);
            writer.flush();
            writer.close();

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    public void createPlaylist(String playlistName){
        try {
            File root = new File(Environment.getExternalStorageDirectory() + File.separator, "Music");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, playlistName + ".m3u");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append("#EXTM3U\n");
            writer.flush();
            writer.close();
            Toast.makeText(getApplicationContext(), "Created " + playlistName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFromPlaylist(String trackName, MusicItem playlist){
        try {
            String line = "";
            String dataToWrite = "";
            String dataToSearch = trackName.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "") + ".mp3";

            File gpxfile = new File( playlist.getPath());

            BufferedReader br = new BufferedReader(new FileReader(gpxfile));
            while ((line = br.readLine()) != null) {
                if(!line.equals(dataToSearch)){
                    dataToWrite += line + "\n";
                }
                else{
                    dataToSearch += "{removed}" + line + "\n";
                }
            }
            br.close();


            FileWriter writer = new FileWriter(gpxfile);
            writer.append(dataToWrite);
            writer.flush();
            writer.close();

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }    }

    public ArrayList<MusicItem> getPlaylistList() {
        return playlistList;
    }

    public String getPlaylistPathFromName(String playlistName){
        for (MusicItem t : playlistList){
            if(t.getTitle().equals(playlistName)){
                return t.getPath();
            }
        }
        return "";
    }

    public MusicItem getTrackFromName(String trackName){
        for (MusicItem t : trackList){
            if(t.getTitle().equals(trackName)){
                return t;
            }
        }
        return null;
    }
    public void setPlaylistList(ArrayList<MusicItem> playlistList) {
        this.playlistList = playlistList;
    }

    public boolean isChangedSongAfterFinish() {
        return changedSongAfterFinish;
    }

    public void setChangedSongAfterFinish(boolean changedSongAfterFinish) {
        this.changedSongAfterFinish = changedSongAfterFinish;
    }

    public boolean isSyncActivated() {
        return isSyncActivated;
    }

    public void setSyncActivated(boolean syncActivated) {
        isSyncActivated = syncActivated;
    }
}