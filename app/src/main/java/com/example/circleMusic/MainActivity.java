package com.example.circleMusic;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    int currentTab = R.id.item2;
    ColorStateList def;
    TextView item1, item2, item3, select;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    String name = "";
    String email = "";
    Uri imageUrl = null;
    Bundle homeScreenBundle;
    RotateAnimation anim;
    ImageView disk;
    boolean isPlaying = false;
    int currentSong = 0;
    public ArrayList<MusicItem> musicItemList;
    public ArrayList<MusicItem> playlistList;
    public ArrayList<MusicItem> currentPlayingList;


    globalCommunication globalCommunicationVar;

    NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contain_main);

        //Request permission
        if (!checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            Toast.makeText(this, "Couldn't get storage permission.", Toast.LENGTH_SHORT).show();
        }

        //Get the signed in Google user
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        //Get user's details
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if(acct != null){
             name = acct.getGivenName();
             email = acct.getEmail();
             imageUrl = acct.getPhotoUrl();
        }

        //Initiate the global track lists
        musicItemList = new ArrayList<>();
        currentPlayingList = new ArrayList<>();
        playlistList = new ArrayList<>();
        globalCommunicationVar = (globalCommunication)getApplicationContext();
        currentSong = globalCommunicationVar.getCurrentPlaying();
        isPlaying = globalCommunicationVar.isPlaying();

        if(globalCommunicationVar.getTrackList() == null || globalCommunicationVar.getTrackList().size() == 0)
        {
            globalCommunicationVar.setTrackList(musicItemList);
            globalCommunicationVar.setCurrentPlayingList(musicItemList);
            globalCommunicationVar.setPlaylistList(playlistList);
            currentPlayingList = globalCommunicationVar.getCurrentPlayingList();
        }
        else {
            musicItemList.addAll(globalCommunicationVar.getTrackList());
            currentPlayingList = globalCommunicationVar.getCurrentPlayingList();
            playlistList.addAll(globalCommunicationVar.getPlaylistList());
        }

        syncServiceManager();

        internalStorageFetch();

        //Create bundle for sending into the fragments
        homeScreenBundle = new Bundle();
        homeScreenBundle.putString("name", name);
        homeScreenBundle.putString("email", email);
        if(imageUrl != null){
            homeScreenBundle.putString("imageUrl", imageUrl.toString());
        }


        smallNowPlayingControl();

        //Update current playing list
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(globalCommunicationVar.isChangedSongAfterFinish()){
                        currentSong = globalCommunicationVar.getCurrentPlaying();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeNowPlayingCard();
                                if(globalCommunicationVar.isPlaying()){
                                    disk.startAnimation(anim);
                                    isPlaying = true;
                                }
                            }
                        });

                        globalCommunicationVar.setChangedSongAfterFinish(false);

                    }
                }

            }
        }).start();


        //Move to now playing activity
        ImageView nowPlaying = findViewById(R.id.nowplayingactivity);
        nowPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nowPlayingIntent = new Intent(MainActivity.this, com.example.circleMusic.nowPlaying.class);
                nowPlayingIntent.putExtra("isPlaying", isPlaying);
                startActivity(nowPlayingIntent);

                //Transaction animation
                overridePendingTransition( R.anim.slide_in_right, R.anim.slide_out_left );
            }
        });

        //Check if now playing returned value
        if(getIntent().getSerializableExtra("returned") != null){

            currentSong = globalCommunicationVar.getCurrentPlaying();
            currentPlayingList = globalCommunicationVar.getCurrentPlayingList();
            isPlaying = globalCommunicationVar.isPlaying();

            //If returned value change the now playing card
            changeNowPlayingCard();

            if(isPlaying)
            {
                disk.startAnimation(anim);
                globalCommunicationVar.play();
            }
        }





        //Animated background
        LinearLayout linearLayout = findViewById(R.id.mainActivity);
        AnimationDrawable animationDrawable = (AnimationDrawable) linearLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);
        animationDrawable.start();

        createChannel(); //Beacuse the build version is higher than 0

        //Manage the tabs
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        homeScreen frag1 = new homeScreen();
        frag1.setArguments(homeScreenBundle);
        ft.add(R.id.fragment_container, frag1);
        ft.commit();

        Toolbar toolbar = new Toolbar(getApplicationContext());
        setSupportActionBar(toolbar);

        item1 = (TextView)findViewById(R.id.item1);
        item2 = (TextView)findViewById(R.id.item2);
        item3 = (TextView)findViewById(R.id.item3);

        item1.setOnClickListener(this);
        item2.setOnClickListener(this);
        item3.setOnClickListener(this);

        select = findViewById(R.id.select);

        def = item1.getTextColors();

        //Swipe gestures for navigation
        findViewById(R.id.mainActivity).setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            public void onSwipeRight() {
                if(currentTab != 0)
                {
                    switch (currentTab)
                    {
                        case R.id.item1:
                            item2.performClick();
                            break;

                        case R.id.item2:
                            item3.performClick();
                            break;

                        case R.id.item3:
                            item1.performClick();
                            break;
                    }
                }
            }
            public void onSwipeLeft() {
                switch (currentTab)
                {
                    case R.id.item1:
                        item3.performClick();
                        break;

                    case R.id.item2:
                        item1.performClick();
                        break;

                    case R.id.item3:
                        item2.performClick();
                        break;
                }
            }

                public void onSwipeBottom() {
                    //Moving to now playing
                    Intent nowPlayingIntent = new Intent(MainActivity.this, com.example.circleMusic.nowPlaying.class);
                    nowPlayingIntent.putExtra("isPlaying", isPlaying);
                    startActivity(nowPlayingIntent);

                    //Transaction animation
                    overridePendingTransition( R.anim.slide_in_right, R.anim.slide_out_left );
                }



        });

    }

    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    private void smallNowPlayingControl(){
        //Small now playing control:

        //Roatate disk animation
        disk = findViewById(R.id.disk);
        anim = new RotateAnimation(0f, 360f, 60f, 60f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(700);

        findViewById(R.id.now_playing).setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            public void onSwipeRight() {
                //previous track
                currentSong = globalCommunicationVar.getCurrentPlaying();
                if(!globalCommunicationVar.isOne()){
                    currentSong--;
                    if(currentSong < 0)
                    {
                        currentSong = currentPlayingList.size() - 1;
                    }
                }

                onClick(); //reversing the action onClick activated

                try {
                    globalCommunicationVar.load(currentPlayingList.get(currentSong).getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                globalCommunicationVar.setCurrentPlaying(currentSong);
                swipeManager();
            }
            public void onSwipeLeft() {
                //next track

                currentSong = globalCommunicationVar.getCurrentPlaying();

                if(!globalCommunicationVar.isOne())
                {
                    currentSong++;
                    if(currentSong >= currentPlayingList.size())
                    {
                        currentSong = 0;
                    }
                }
                onClick(); //reversing the action onClick activated

                try {
                    globalCommunicationVar.load(currentPlayingList.get(currentSong).getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                globalCommunicationVar.setCurrentPlaying(currentSong);
                swipeManager();
            }

            public void onClick() {
                if(isPlaying){
                    isPlaying = false;
                    disk.getAnimation().cancel();

                    globalCommunicationVar.pause();
                }
                else {
                    isPlaying = true;
                    disk.startAnimation(anim);

                    if(!globalCommunicationVar.isLoaded()){ //To load the first song
                        try {
                            globalCommunicationVar.load(currentPlayingList.get(currentSong).getPath());
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(), "Can't load song", Toast.LENGTH_SHORT).show();
                        }
                    }

                    globalCommunicationVar.play();
                }
            }

        });

        //Update now playing if clicked on home screen or search
        new Thread(() -> {
            int currentPlaying = -1;
            int lastPlayed = -1;
            while (true){
                currentPlaying = globalCommunicationVar.getCurrentPlaying();
                if(currentPlaying != lastPlayed){

                    currentPlayingList = globalCommunicationVar.getCurrentPlayingList();
                    currentSong = globalCommunicationVar.getCurrentPlaying();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeNowPlayingCard();
                            if(globalCommunicationVar.isPlaying()){
                                disk.startAnimation(anim);
                            }
                        }
                    });

                    lastPlayed = currentPlaying;
                }

            }
        }).start();

    }

    private void syncServiceManager(){
        //Start playing service - Only if data syncing from the memory finished and only if the device is not low on memory and if the option is activated
        if(globalCommunicationVar.isSyncActivated())
        {
            ActivityManager.MemoryInfo memoryInfo = getAvailableMemory();
            try {
                if (globalCommunicationVar.isFinishedSyncing() && !memoryInfo.lowMemory) {
                    Intent backgroundSyncIntent = new Intent(this, backgroundSync.class);
                    backgroundSyncIntent.putExtra("useremail", email);
                    startService(backgroundSyncIntent);
                }
            }
            catch(Exception e){
                Log.d("Error", "Could not sync with database");
                Toast.makeText(getApplicationContext(), "Could not sync with database", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getPlaylists(File dir) throws IOException {
        String playlistPattern = ".m3u";
        File listFile[] = dir.listFiles();

        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {

                if (listFile[i].isDirectory()) {
                    getPlaylists(listFile[i]);
                } else {
                    if (listFile[i].getName().endsWith(playlistPattern)){
                        if(!(listFile[i].getName().contains("(") && listFile[i].getName().contains(")"))){
                            MusicItem playlist = new MusicItem(listFile[i].getName().replace(".m3u", ""), "Playlist", -1, false, false, listFile[i].getPath(), 0);
                            globalCommunicationVar.addPlaylist(playlist);

                        }

                    }
                }
            }
        }
    }

    private void internalStorageFetch(){
        //Internal storage fetch
        if(!globalCommunicationVar.isFinishedSyncing() && !globalCommunicationVar.isCurrentlySyncing())
        {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    globalCommunicationVar.setFinishedSyncing(false);
                    globalCommunicationVar.setCurrentlySyncing(true);
                    ContentResolver contentResolver = getContentResolver();
                    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
                    String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
                    Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
                    if (cursor != null && cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            @SuppressLint("Range") int duration = Integer.parseInt(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))) / 1000;
                            //In seconds
                            @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                            // Save to audioList
                            if(!title.contains("AUD")) { //Dont add recordings
                                musicItemList.add(new MusicItem(title, artist, getImage(path), false, true, path, duration));
                            }
                        }
                    }
                    globalCommunicationVar.setCurrentlySyncing(false);
                    globalCommunicationVar.setFinishedSyncing(true);
                    Objects.requireNonNull(cursor).close();
                    return;
                }
            }).start();

            //fetch internal storage playlists
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        getPlaylists(Environment.getExternalStorageDirectory());
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Problem getting playlists", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }).start();
        }
    }

    private void swipeManager() {
        //Load track
        try {
            globalCommunicationVar.load(currentPlayingList.get(currentSong).getPath());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Problem loading song", Toast.LENGTH_SHORT).show();
        }

        //Play song if playing
        if(isPlaying)
        {
            disk.startAnimation(anim);
            globalCommunicationVar.play();
        }


    }

    private void changeNowPlayingCard() {
        currentSong = globalCommunicationVar.getCurrentPlaying();

        TextView title = findViewById(R.id.nowPlayingTitle);
        TextView subTitle = findViewById(R.id.nowPlayingSubTitle);
        ImageView image = findViewById(R.id.nowPlayinImage);

        if (globalCommunicationVar.getCurrentPlayingList().size() != 0) {
            title.setText(globalCommunicationVar.getCurrentPlayingList().get(currentSong).getTitle());
            subTitle.setText(globalCommunicationVar.getCurrentPlayingList().get(currentSong).getSubTitle());
            image.setImageDrawable(((globalCommunication)getApplicationContext()).getImage(globalCommunicationVar.getCurrentPlayingList().get(currentSong).getImageId()));
        }
    }


    @Override
    public void finish(){
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private int getImage(String path)
    {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);

        byte [] data = mmr.getEmbeddedPicture();

        if(data != null)
        {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Drawable d = new BitmapDrawable(getResources(), bitmap);
            return globalCommunicationVar.addImage(d);
        }

        return -1; //Application logo image



    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(createNotification.CHANNEL_ID, "Kod Dev", NotificationManager.IMPORTANCE_LOW);

        notificationManager = getSystemService(NotificationManager.class);
        if(notificationManager != null)
        {
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public void onClick(View view) {
        currentTab = view.getId();
        //change animation
        if(view.getId() == R.id.item1) {

            select.animate().x(0).setDuration(200);
            item1.setTextColor(Color.WHITE);
            item2.setTextColor(def);
            item3.setTextColor(def);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            artists artists = new artists();
            ft.replace(R.id.fragment_container,artists);
            ft.commit();
        } else if (view.getId() == R.id.item2) {

            select.animate().x(item2.getWidth()).setDuration(200);
            item2.setTextColor(Color.WHITE);
            item1.setTextColor(def);
            item3.setTextColor(def);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            homeScreen hs = new homeScreen();
            hs.setArguments(homeScreenBundle);
            ft.replace(R.id.fragment_container,hs);
            ft.commit();
        } else if (view.getId() == R.id.item3) {

            select.animate().x(item3.getWidth() + item2.getWidth()).setDuration(200);
            item3.setTextColor(Color.WHITE);
            item2.setTextColor(def);
            item1.setTextColor(def);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            search search = new search();
            ft.replace(R.id.fragment_container,search);
            ft.commit();

        }
        else
        {
            currentTab = R.id.item2;
        }

    }


    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    private void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }
}







