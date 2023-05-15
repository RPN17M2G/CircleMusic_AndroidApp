package com.example.circleMusic;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class nowPlaying extends AppCompatActivity {

    public ArrayList<MusicItem> musicItemList; //Can get shuffeled
    public ArrayList<MusicItem> musicItemListCopy;
    public int currentItem;
    public boolean isOne = false;
    public boolean isShuffle = false;
    private SeekBar seekBar;
    private TextView indicator;
    private TextView title;
    private TextView artist;
    private ImageView image;
    private Runnable seekbarEverySecond;
    private boolean isFirstTime = true;
    private boolean onlyEnteredNowPlaying = true;
    private globalCommunication globalLists;
    private boolean isPlayingForMovingSong = false;
    private boolean stateOfPlayButton = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        //Get global class
        globalLists = ((globalCommunication)getApplicationContext());

        //Get data from Intent
        musicItemList = globalLists.getCurrentPlayingList();

        //Add a temporary item to musicItemList if the list is empty
        if(musicItemList.size() == 0)
        {
            musicItemList.add(new MusicItem("No music playing", "", ((globalCommunication) getApplicationContext()).addImage(getDrawable(R.drawable.neonmusicicon)), false, false, "", 0));
        }

        musicItemListCopy = new ArrayList<>();
        musicItemListCopy.addAll(musicItemList);

        currentItem = globalLists.getCurrentPlaying();
        isShuffle = globalLists.isShuffle();
        isOne = globalLists.isOne();

        //Get ui elements
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        image = findViewById(R.id.image);

        //Set back button
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Setting the deatils of the song
        setDuration();
        setSongDetails();
        manageSeekbar();
        seekBar.setProgress(globalLists.getProgress());


        //Set control media buttons
        ImageView shuffle = findViewById(R.id.shuffle);
        ImageView last = findViewById(R.id.last);
        ImageView play = findViewById(R.id.play);
        ImageView next = findViewById(R.id.next);
        ImageView one = findViewById(R.id.one);

        //If the song plays
        if(globalLists.isPlaying()){
            play.setImageResource(R.drawable.pausesong);
            isPlayingForMovingSong = true;
            stateOfPlayButton = true;
        }
        //If shuffle is on
        if(isShuffle)
        {
            shuffle.setImageResource(R.drawable.shufflesong);
        }
        //If one is on
        if(isOne)
        {
            one.setImageResource(R.drawable.onepressed);
        }


        //Add swipe gestures
        findViewById(R.id.nowplayingbackground).setOnTouchListener(new OnSwipeTouchListener(nowPlaying.this) {
            public void onSwipeRight() {
                next.performClick();
            }

            public void onSwipeLeft() {
                last.performClick();
            }

            });

        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_click));
                if(globalLists.isPlaylistShuffle()){
                    Toast.makeText(getApplicationContext(), "Can't unshuffle because shuffle source is from playlist, please play a playlist regularly for enabling unshuffle option", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isShuffle) {
                    shuffle.setImageResource(R.drawable.noshuffle);

                    //Returning the user to the normal list by finding the current song
                    currentItem = findItemIndexInCopy(musicItemList.get(currentItem).getTitle());

                    musicItemList.clear(); //Returning to the original list
                    musicItemList.addAll(musicItemListCopy);
                } else {
                    shuffle.setImageResource(R.drawable.shufflesong);
                    shuffle(musicItemList);
                }
                isShuffle = !isShuffle;
                globalLists.setShuffle(isShuffle);
            }
        });

        last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_click));
                if(!isOne) //If its one dont change the song just return the user to the start of the song
                {
                    currentItem--;
                    if (currentItem < 0) {
                        currentItem = musicItemList.size() - 1;
                    }
                }



                //Load track
                try {
                    globalLists.load(musicItemList.get(currentItem).getPath());
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Problem loading song", Toast.LENGTH_SHORT).show();
                }

                //Update track details
                setDuration();
                setSongDetails();
                manageSeekbar();

                //Play song if playing
                if(isPlayingForMovingSong)
                {
                    globalLists.play();
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_click));
                if (globalLists.isPlaying()) {
                    play.setImageResource(R.drawable.playsong);

                    globalLists.pause();
                    isPlayingForMovingSong = false;
                } else {
                    play.setImageResource(R.drawable.pausesong);

                    if(!globalLists.isLoaded()){ //After problem loading first song
                        try {
                            globalLists.load(musicItemList.get(currentItem).getPath());
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(), "Can't load song", Toast.LENGTH_SHORT).show();
                        }
                    }
                    globalLists.play();
                    isPlayingForMovingSong = true;
                }
                stateOfPlayButton = !stateOfPlayButton;
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_click));
                nextSong();
            }
        });

        one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_click));
                if (isOne) {
                    one.setImageResource(R.drawable.onenonpressed);
                } else {
                    one.setImageResource(R.drawable.onepressed);
                }
                isOne = !isOne;
                globalLists.setOne(isOne);
            }
        });



        addTrackToPlaylist();
    }

    private void addTrackToPlaylist(){
        //Add track to playlist
        ImageView add = findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.track_click));

                PopupMenu popupMenu = new PopupMenu(nowPlaying.this, add, Gravity.CENTER, R.style.PopupTheme, R.style.PopupTheme);

                popupMenu.getMenu().add("Create Playlist");

                for(MusicItem playlist : ((globalCommunication)getApplicationContext()).getPlaylistList()){
                    popupMenu.getMenu().add(playlist.getTitle());
                }

                popupMenu.show();

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String playlistName = item.getTitle().toString();
                        if (playlistName.equals("Create Playlist")) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(nowPlaying.this);
                            builder.setTitle("Playlist Name:");

                            final EditText input = new EditText(nowPlaying.this);
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            builder.setView(input);


                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((globalCommunication) getApplicationContext()).createPlaylist(input.getText().toString());
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            builder.show();

                        }

                        ((globalCommunication) getApplicationContext()).addToPlaylist(musicItemList.get(currentItem), playlistName);

                        return true;
                    }
                });
            }
        });
    }


    private int findItemIndexInCopy(String title) {
        for(int i = 0; i < musicItemListCopy.size(); i++)
        {
            if(title.equals(musicItemListCopy.get(i).getTitle())){
                return i;
            }
        }
        return 0;
    }

    private void setDuration() {
        //Set duration
        int minutes = (musicItemList.get(currentItem).getDuration() % 3600) / 60;
        int seconds = musicItemList.get(currentItem).getDuration() % 60;

        String minutesZero = "";
        String secondsZero = "";

        if(minutes < 10){
            minutesZero = "0";
        }
        if(seconds < 10){
            secondsZero = "0";
        }

        ((TextView)findViewById(R.id.duration)).setText(minutesZero + minutes + ":" + secondsZero + seconds);
    }

    private void setSongDetails()
    {
        //Set song details
        image.setImageDrawable(globalLists.getImage(musicItemList.get(currentItem).getImageId()));
        title.setText(musicItemList.get(currentItem).getTitle());
        artist.setText(musicItemList.get(currentItem).getSubTitle());

        //Apply dominant color to the background
        int imageColor = getImageColor();
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{imageColor, getColor(R.color.dark_gray_real), getColor(R.color.black)});
        gd.setCornerRadius(0f);
        LinearLayout nowPlayingView = findViewById(R.id.nowplayingbackground);
        nowPlayingView.setBackground(gd);
    }

    private int getImageColor(){
        Bitmap bm = null;
        if(image.getDrawable() instanceof LayerDrawable)
        {
            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            bm = Bitmap.createBitmap(200, 200 ,conf);
            image.getDrawable().draw(new Canvas(bm));
        }
        else if(image.getDrawable() instanceof Drawable) {
            bm = ((BitmapDrawable) (image.getDrawable())).getBitmap();
        }
        else{
            bm = ((BitmapDrawable) (getDrawable(R.drawable.neonmusicicon))).getBitmap();
        }
        return getDominantColor(bm);
    }

    private void manageSeekbar(){
        seekBar = findViewById(R.id.seekbar);
        indicator = findViewById(R.id.seekbarindicator);

        seekBar.setMax(musicItemList.get(currentItem).getDuration() + 1);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new seekbarListener());

        //Change seekbar line color to the track image color
        seekBar.getProgressDrawable().setColorFilter(getImageColor(), PorterDuff.Mode.MULTIPLY);

        //Do the program need to start the thread that is responsible for moving the seekbar or is it already working/
        if(isFirstTime){
            isFirstTime = false;
            //Make seekbar move foward every second by using Thread
            Handler mHandler = new Handler();
            seekbarEverySecond = new Runnable() {

                @Override
                public void run() {
                    int progress = seekBar.getProgress();
                    if(stateOfPlayButton) {
                        progress = seekBar.getProgress() + 1;
                        seekBar.setProgress(progress);
                    }
                    if(!globalLists.isPlaying() && stateOfPlayButton)
                    {
                        globalLists.pause();
                    }
                    if(progress >= seekBar.getMax()){
                        seekBar.setProgress(0);
                        isPlayingForMovingSong = true;
                        nextSong();
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };

            nowPlaying.this.runOnUiThread(seekbarEverySecond);
        }
    }

    private void nextSong() {

        if(!isOne) //If its one dont change the song just return the user to the start of the song
        {
            currentItem++;
            if(currentItem > musicItemList.size() - 1) { currentItem = 0; }
        }

        //Load track
        try {
            globalLists.load(musicItemList.get(currentItem).getPath());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Problem loading song", Toast.LENGTH_SHORT).show();
        }

        //Update track details
        setDuration();
        setSongDetails();
        manageSeekbar();

        if(isPlayingForMovingSong){
            globalLists.play();
        }

        globalLists.setCurrentPlaying(currentItem);
        globalLists.setChangedSongAfterFinish(true);

    }

    private ArrayList<MusicItem> shuffle(ArrayList<MusicItem> musicItemList) {
        int src = 0;
        int dest = 0;
        MusicItem temp;

        //Fisher–Yates shuffle Algorithm
        for (int i = musicItemList.size() - 1; i > 0; i--) {
            int dst = new Random().nextInt(musicItemList.size());

            temp = musicItemList.get(i);
            musicItemList.set(i, musicItemList.get(dst));
            musicItemList.set(dst, temp);
        }

        return musicItemList;
    }


    private static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    @Override
    public void finish() {
        super.finish();
        onlyEnteredNowPlaying = true;

        Intent mainActivityIntent = new Intent(nowPlaying.this, MainActivity.class);
        globalLists.setCurrentPlaying(currentItem);
        globalLists.setCurrentPlayingList(musicItemList);
        mainActivityIntent.putExtra("returned", true);
        startActivity(mainActivityIntent);

        //Transaction animation
        overridePendingTransition( R.anim.slide_in_left, R.anim.slide_out_right );    }

    private class seekbarListener implements SeekBar.OnSeekBarChangeListener {

        private int lastProgress = 0;
        public seekbarListener(){}

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            int minutes = (progress % 3600) / 60;
            int seconds = progress % 60;

            String minutesZero = "";
            String secondsZero = "";

            if(minutes < 10){
                minutesZero = "0";
            }
            if(seconds < 10){
                secondsZero = "0";
            }

            indicator.setText(minutesZero + minutes + ":" + secondsZero + seconds);

            if((progress > lastProgress + 2 || progress < lastProgress - 2) && !onlyEnteredNowPlaying){ //Only if a jump that is more than a second
                globalLists.setProgress(progress + 1);
            }
            onlyEnteredNowPlaying = false;
            lastProgress = progress;
        }

        public void onStartTrackingTouch(SeekBar seekBar) {}

        public void onStopTrackingTouch(SeekBar seekBar) {}

    }
}


