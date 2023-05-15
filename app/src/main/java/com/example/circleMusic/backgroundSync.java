package com.example.circleMusic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;

public class backgroundSync extends Service {

    private globalCommunication globalLists;
    private ArrayList<PlaylistItem> playlistItems;
    private ArrayList<PlaylistItem> playlistExistsItems;
    private ArrayList<TrackItem> trackItems;
    private TrackItem trackItem = null;
    private boolean finished_playlists = false;
    private boolean finished_tracks = false;
    private MusicItem currentTrack = null;
    private ID3v2 id3v2Tag = null;
    private Thread serviceThread = null;
    private Thread trackSendThread = null;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //-1 will force getImage to return the icon image of the app
        createNotification.createNotification(getApplicationContext(), "Started cloud database synchronization", "", -1);

        serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                globalLists = ((globalCommunication) getApplicationContext());

                // returns the status
                // of the program
                while (!isOnline(getApplicationContext())) {
                    //Offline
                    SystemClock.sleep(300_000);
                }

                while (!globalLists.isFinishedSyncing()) {
                    //Didnt finished syncing
                    SystemClock.sleep(10_000);
                }

                DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();

                String email = intent.getSerializableExtra("useremail").toString(); //Email in hash used as a key to the right dataset on the database
                String hashedEmail = hashString(email);

                if (hashedEmail.equals("START_STICKY")) {
                    return;
                }

                //Get names of tracks
                ArrayList<String> trackNamesList = new ArrayList<>();
                for (MusicItem track : globalLists.getTrackList()) {
                    trackNamesList.add(track.getTitle().replaceAll("[., #, $, [, ]]", ""));
                }

                //Get names of playlists
                ArrayList<String> playlistsNamesList = new ArrayList<>();
                for (MusicItem track : globalLists.getPlaylistList()) {
                    playlistsNamesList.add(track.getTitle().replaceAll("[., #, $, [, ]]", ""));
                }

                //Get database playlists
                playlistExistsItems = new ArrayList<>();
                playlistItems = new ArrayList<>();

                myRef.child("playlists").child(hashedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (!playlistsNamesList.contains(snapshot.getKey())) {
                                playlistItems.add(new PlaylistItem(snapshot.getKey(), (String) snapshot.getValue()));
                                Log.d("DEBUG", "entrered");
                            } else {
                                playlistExistsItems.add(new PlaylistItem(snapshot.getKey(), (String)snapshot.getValue()));
                                playlistsNamesList.remove(snapshot.getKey()); //Remove the playlist because it is already exists in the storage
                            }
                        }
                        finished_playlists = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        return;
                    }
                });


                //Get database tracks
                trackItems = new ArrayList<>();
                myRef.child("tracks").child(hashedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (!trackNamesList.contains(snapshot.getKey())) {
                                trackItem = new TrackItem(snapshot.getKey());
                                trackItems.add(trackItem);
                                myRef.child("tracks").child(hashedEmail).child(snapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            trackItem.addToField(snapshot.getKey(), snapshot.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            } else {
                                trackNamesList.remove(snapshot.getKey()); //Remove the track because it is already exists in the storage
                            }
                        }
                        finished_tracks = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //Wait until finished syncing the database data
                while (!(finished_playlists && finished_tracks)) {
                    SystemClock.sleep(10_000);
                }

                //Send to database all the non existing playlists
                //PlaylistsNamesList contains all playlists that exists in the device but doesnt exists in the database
                String playlistValue = "";
                for (String playlistName : playlistsNamesList) {
                    try {
                        playlistValue = "";
                        String line = "";

                        File gpxfile = new File(globalLists.getPlaylistPathFromName(playlistName));

                        BufferedReader br = new BufferedReader(new FileReader(gpxfile));
                        while ((line = br.readLine()) != null) {
                            playlistValue += line + "\n";
                        }
                        br.close();



                        FileWriter writer = new FileWriter(gpxfile);
                        writer.write(playlistValue);
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        Log.e("Exception", "File read failed: " + e.toString());
                    }

                    myRef.child("playlists").child(hashedEmail).child(playlistName).setValue(playlistValue);
                }

                //Save to internal storage all of the non existing playlists
                for (PlaylistItem playlistItem : playlistItems) {
                    try {
                        File root = new File(Environment.getExternalStorageDirectory() + File.separator, "Music");
                        if (!root.exists()) {
                            root.mkdirs();
                        }
                        File gpxfile = new File(root, playlistItem.getTitle() + ".m3u");
                        FileWriter writer = new FileWriter(gpxfile);
                        writer.append(playlistItem.getData());
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //Check for changes in the existing playlists
                String playlistSyncedFinalValue = "";
                for(PlaylistItem playlistItem : playlistExistsItems){
                    try {
                        playlistValue = "";
                        String line = "";
                        playlistSyncedFinalValue = "";

                        File gpxfile = new File(globalLists.getPlaylistPathFromName(playlistItem.getTitle()));

                        BufferedReader br = new BufferedReader(new FileReader(gpxfile));
                        while ((line = br.readLine()) != null) {
                            playlistValue += line + "\n";
                        }
                        br.close();

                        Scanner scanner = new Scanner(playlistItem.getData());
                        while (scanner.hasNextLine()) {
                            line = scanner.nextLine();
                            if(line.contains("{removed}")){
                                line.replace("{removed}", "");
                                playlistValue.replace(line, "");
                            }
                            else if(!playlistValue.contains(line)){
                                playlistValue += line + "\n";
                            }
                        }
                        scanner.close();

                        scanner = new Scanner(playlistValue);
                        while (scanner.hasNextLine()) {
                            line = scanner.nextLine();

                            if(!line.contains("{removed}")){
                                playlistSyncedFinalValue += line + "\n";
                            }
                        }
                        scanner.close();

                        FileWriter writer = new FileWriter(gpxfile);
                        writer.write(playlistSyncedFinalValue);
                        writer.flush();
                        writer.close();

                    } catch (IOException e) {
                        Log.e("Exception", "File action failed: " + e.toString());
                    }

                    myRef.child("playlists").child(hashedEmail).child(playlistItem.getTitle()).setValue(playlistSyncedFinalValue);

                }

                //Send to database all the non existing tracks
                //trackNamesList contains all the names of the tracks that the database doesn't have
                //There is a Thread for saving the tracks on the database becuase it takes time
                trackSendThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UploadTask uploadTask = null;
                        try {
                            for (String trackName : trackNamesList) {
                                currentTrack = globalLists.getTrackFromName(trackName);
                                if (currentTrack != null) {
                                    //Upload mp3 file
                                    Uri file = Uri.fromFile(new File(currentTrack.getPath()));
                                    StorageReference dataReference = storageRef.child("audio/" + file.getLastPathSegment());
                                    uploadTask = dataReference.putFile(file);

                                    // Register observers to listen for when the download is done or if it fails
                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle unsuccessful uploads
                                            Log.d("DEBUG", exception.getMessage());
                                        }
                                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            myRef.child("tracks").child(hashedEmail).child(trackName).child("Data").setValue("audio/" + file.getLastPathSegment());

                                        }
                                    });


                                    //Upload image
                                    Bitmap bitmap = ((BitmapDrawable) globalLists.getImage(currentTrack.getImageId())).getBitmap();
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream); //reduce quality of image for better storage managment
                                    byte[] bitmapdata = stream.toByteArray();

                                    StorageReference imageReference = storageRef.child("image/" + file.getLastPathSegment());
                                    uploadTask = imageReference.putBytes(bitmapdata);

                                    // Register observers to listen for when the download is done or if it fails
                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle unsuccessful uploads
                                        }
                                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            myRef.child("tracks").child(hashedEmail).child(trackName).child("Image").setValue("image/" + file.getLastPathSegment());

                                        }
                                    });

                                    //Upload Artist
                                    myRef.child("tracks").child(hashedEmail).child(trackName).child("Artist").setValue(currentTrack.getSubTitle());


                                }
                            }
                        }
                        catch(java.util.ConcurrentModificationException e){
                            Log.e("Error", e.toString());
                        }
                    }
                });
                trackSendThread.start();




                //Save all non existing tracks from the database
                StorageReference audioRef = null;
                StorageReference imageRef = null;
                Byte[] imageData = null;
                final long TWENTY_MEGABYTE = 1024 * 1024 * 20; //Most audio files are no bigger than 20 mb
                for (TrackItem trackItem : trackItems) {

                    if(trackItem != null && trackItem.isInitalized()){
                        //Creating the mp3 file
                        File root = new File(Environment.getExternalStorageDirectory() + File.separator, "Music");
                        if (!root.exists()) {
                            root.mkdirs();
                        }
                        File gpxfile = new File(root, trackItem.getTitle() + ".mp3");

                        //Opening the tag editor
                        Mp3File mp3file = null;
                        try {
                            mp3file = new Mp3File(gpxfile.toPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (UnsupportedTagException e) {
                            e.printStackTrace();
                        } catch (InvalidDataException e) {
                            e.printStackTrace();
                        }

                        if (mp3file != null && mp3file.hasId3v2Tag()) {
                            id3v2Tag = mp3file.getId3v2Tag();
                        } else if(mp3file != null){
                            // mp3 does not have an ID3v2 tag
                            id3v2Tag = new ID3v24Tag();
                            mp3file.setId3v2Tag(id3v2Tag);
                        }


                        //Getting mp3 data
                        audioRef = storageRef.child(trackItem.getData());

                        audioRef.getBytes(TWENTY_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                try {

                                    Files.write(gpxfile.toPath(), bytes); //Writing the data of the file to the file

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                            }
                        });

                        //Getting image
                        imageRef = storageRef.child(trackItem.getImage());

                        imageRef.getBytes(TWENTY_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {

                                id3v2Tag.setAlbumImage(bytes, "JPG"); //set album image

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                            }
                        });



                        //Add mp3 tag
                        if(id3v2Tag != null){
                            id3v2Tag.setArtist(trackItem.getArtist());
                            id3v2Tag.setTitle(trackItem.getTitle());
                        }


                    }

                }
            }
        });
        serviceThread.start();


        //Join the thread so the notification will show only when the sync is finished
        try {
            serviceThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //-1 will force getImage to return the icon image of the app
        createNotification.createNotification(getApplicationContext(), "Finished cloud database synchronization", "", -1);

        return START_STICKY;
    }

    @Override
    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        try{
            //Stopping the threads
            trackSendThread.stop();
            serviceThread.stop();
        }
        catch (Exception e){

        }
        //-1 will force getImage to return the icon image of the app
        createNotification.createNotification(getApplicationContext(), "Stopped cloud database synchronization", "", -1);

        super.onDestroy();
        // stopping the process
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    private byte[] readMp3File(String filePath){
        File file = new File(filePath);
        byte[] fileData = new byte[(int) file.length()];
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            assert dis != null;
            dis.readFully(fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileData;
    }

    private final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String hashString(String toHash){

        //Get email hashed so that the Email of the user wont be saved in the database
        //To prevent security leaks - used SHA-256 Hash function algorithm
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(sha256 == null){
            return "START_STICKY"; //Stop the background sync if hash doesn't work
        }
        sha256.reset();
        sha256.update(toHash.getBytes(StandardCharsets.UTF_8));
        byte[] output = sha256.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte b : output) {
            hexString.append(Integer.toHexString(0xFF & b));
        }

        return hexString.toString();
    }



    private class PlaylistItem {
        private String title;
        private String data;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }


        public PlaylistItem(String title, String data) {
            this.title = title;
            this.data = data;
        }

    }



    private class TrackItem {
        private String title;
        private String artist;
        private String data;
        private String image;

        public TrackItem(String title){
            this.title = title;
            artist = "";
            data = "";
            image = "";

        }

        public void addToField(String key, String value) {

            switch (key){
                case "Title":
                    title = value;
                    break;
                case "Artist":
                    artist = value;
                    break;
                case "Data":
                    data = value;
                    break;
                case "Image":
                    image = value;
                    break;
            }
        }

        public boolean isInitalized(){
            return title != null && !title.equals("") &&
                    artist != null && !artist.equals("") &&
                    data != null && !data.equals("") &&
                    image != null && !image.equals("");
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getData() {
            return data;
        }

        public String getImage() {
            return image;
        }

    }

}
