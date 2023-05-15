package com.example.circleMusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class playlist extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<MusicItem> filteredMusicItems;
    private LinearLayoutManager linearLayoutManager;
    private playlist.MyRvAdapter myRvAdapter;
    private ImageView subjectImage;
    private MusicItem playtlistItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);


        //Get data from Intent
        ArrayList<MusicItem> musicItemList = ((globalCommunication)getApplicationContext()).getTrackList();
        playtlistItem = (MusicItem) getIntent().getSerializableExtra("playlistItem");

        filteredMusicItems = new ArrayList<>();

        if(playtlistItem.getSubTitle().equals("Artist")) {
            for (int i = 0; i < musicItemList.size(); i++) {
                if (musicItemList.get(i).getSubTitle().contains(playtlistItem.getTitle())) {
                    filteredMusicItems.add(musicItemList.get(i));
                }
            }
        }
        else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(playtlistItem.getPath()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            ArrayList<String> m3uFileList = new ArrayList<>();
            String line;
            boolean firstLine = true; //first line = #extm3u
            try {
                if(bufferedReader != null){
                    while ((line = bufferedReader.readLine()) != null) {
                        if(!firstLine && !line.contains("{removed}")){
                            m3uFileList.add(line.substring(line.lastIndexOf("/") + 1).trim().toLowerCase(Locale.ROOT).replace(" .mp3", "").replace(".mp3", ""));
                        }
                        else{
                            firstLine = false;
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            try {
                for(MusicItem musicItem : musicItemList){
                    specificTrack:
                    for(String name : m3uFileList){
                        if(name.equals(musicItem.getTitle().toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", ""))){
                            filteredMusicItems.add(musicItem);
                            break specificTrack;
                        }
                    }
                }
            }
            catch (java.util.ConcurrentModificationException e){
                Toast.makeText(getApplicationContext(), "Currently syncing playlist error, please try again", Toast.LENGTH_SHORT).show();
            }


        }

        recyclerView = findViewById(R.id.tracksPlaylistRecyclerView);


        //Add data to the recycler view
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        myRvAdapter = new playlist.MyRvAdapter(filteredMusicItems);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);



        //Apply playlist image
        subjectImage = findViewById(R.id.subjectImage);
        if(filteredMusicItems.size() >= 1){
            subjectImage.setImageDrawable(((globalCommunication)getApplicationContext()).getImage(filteredMusicItems.get(0).getImageId()));
        }
        else{
            subjectImage.setImageDrawable(((globalCommunication)getApplicationContext()).getImage(playtlistItem.getImageId()));
        }

        //Apply dominant color of playlist image to the background
        Bitmap bm=((BitmapDrawable)subjectImage.getDrawable()).getBitmap();
        int imageColor = getDominantColor(bm);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {imageColor,getColor(R.color.dark_gray_real), getColor(R.color.black)});
        gd.setCornerRadius(0f);
        NestedScrollView playlistView = findViewById(R.id.playlistbackground);
        playlistView.setBackground(gd);


        //Change playlist name and type
        TextView title = findViewById(R.id.Title);
        TextView subTitle = findViewById(R.id.subTitle);

        title.setText(playtlistItem.getTitle());
        subTitle.setText("Number of songs: " + filteredMusicItems.size());


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        LinearLayout play = findViewById(R.id.playButton);
        LinearLayout shuffle = findViewById(R.id.shuffleButton);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.track_click));

                ((globalCommunication)getApplicationContext()).setPlaylistShuffle(false);
                ((globalCommunication)getApplicationContext()).setShuffle(false);
                ((globalCommunication)getApplicationContext()).setCurrentPlayingList(filteredMusicItems);
                ((globalCommunication)getApplicationContext()).setCurrentPlaying(0);

                try {
                    ((globalCommunication)getApplicationContext()).load(filteredMusicItems.get(0).getPath());
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Problem loading song", Toast.LENGTH_SHORT).show();
                }

                ((globalCommunication)getApplicationContext()).play();

            }
        });

        shuffle.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.track_click));

            ((globalCommunication)getApplicationContext()).setPlaylistShuffle(true);
            ((globalCommunication)getApplicationContext()).setShuffle(true);
            ((globalCommunication)getApplicationContext()).setCurrentPlayingList(shuffle(filteredMusicItems));
            ((globalCommunication)getApplicationContext()).setCurrentPlaying(0);

            try {
                ((globalCommunication)getApplicationContext()).load(filteredMusicItems.get(0).getPath());
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Problem loading song", Toast.LENGTH_SHORT).show();
            }

            ((globalCommunication)getApplicationContext()).play();

        });

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

    class MyRvAdapter extends RecyclerView.Adapter<playlist.MyRvAdapter.MyHolder> {


        ArrayList<MusicItem> musicItemArrayList;

        public MyRvAdapter(ArrayList<MusicItem> musicItemArrayList) {
            this.musicItemArrayList = musicItemArrayList;
        }

        @NonNull
        @Override
        public playlist.MyRvAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.playlistverticalitem, parent, false);
            return new playlist.MyRvAdapter.MyHolder(view);
        }


        @SuppressLint("NotifyDataSetChanged")
        public void onBindViewHolder(@NonNull playlist.MyRvAdapter.MyHolder holder, int position) {
            holder.title.setText(musicItemArrayList.get(position).getTitle());
            holder.image.setImageDrawable(((globalCommunication)getApplicationContext()).getImage(musicItemArrayList.get(position).getImageId()));
            holder.subTitle.setText(musicItemArrayList.get(position).getSubTitle());

            holder.remove.setOnClickListener(view->{
                view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.track_click));
                ((globalCommunication)getApplicationContext()).removeFromPlaylist(musicItemArrayList.get(position).getTitle(), playtlistItem);
                filteredMusicItems.remove(musicItemArrayList.get(position));
                recyclerView.getAdapter().notifyDataSetChanged();
                TextView subTitle = findViewById(R.id.subTitle);
                subTitle.setText("Number of songs: " + filteredMusicItems.size());
            });

            holder.itemView.setOnClickListener(view ->{
                view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.track_click));

                ((globalCommunication)getApplicationContext()).setPlaylistShuffle(false);
                ((globalCommunication)getApplicationContext()).setShuffle(false);
                ((globalCommunication)getApplicationContext()).setCurrentPlayingList(filteredMusicItems);
                ((globalCommunication)getApplicationContext()).setCurrentPlaying(((globalCommunication)getApplicationContext()).getTrackPositionFromName(filteredMusicItems.get(position)));
                try {
                    ((globalCommunication)getApplicationContext()).load(musicItemArrayList.get(position).getPath());
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Problem loading track", Toast.LENGTH_SHORT).show();
                }
                ((globalCommunication)getApplicationContext()).play();
            });
            return;
        }


        @Override
        public int getItemCount() {

            return musicItemArrayList.size();
        }


        class MyHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subTitle;
            ImageView image;
            ImageView remove;

            public MyHolder(@NonNull View itemView) {
                super(itemView);
                this.title = itemView.findViewById(R.id.title);
                this.image = itemView.findViewById(R.id.image);
                this.subTitle = itemView.findViewById(R.id.subTitle);
                this.remove = itemView.findViewById(R.id.remove);

            }
        }
    }
}