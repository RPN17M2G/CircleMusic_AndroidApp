package com.example.circleMusic;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;


public class homeScreen extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<MusicItem> Playlists;
    private ArrayList<MusicItem> Special;
    private ArrayList<MusicItem> musicItems; //tracks should be availeble in the playlists and artists section
    private LinearLayoutManager linearLayoutManager;
    private MyRvAdapter myRvAdapter;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    private String playlistName = "";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_screen, container, false);

        //Get user's details
        String name = getArguments().getString("name");
        String email = getArguments().getString("email");
        String imageUrl = getArguments().getString("imageUrl");

        musicItems = (((globalCommunication)getActivity().getApplicationContext()).getTrackList());

        setGoogleProfilePicture(imageUrl, view);

        TextView welcomeText = view.findViewById(R.id.subTitle);
        welcomeText.setText("Welcome back " + name + "!");

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(getActivity(), gso);

        googleImageListener(view);

        //Add data to the Playlists recycler view
        recyclerView = view.findViewById(R.id.PlaylistsRecyclerView);

        Playlists = new ArrayList<>();
        Playlists.addAll(((globalCommunication)getActivity().getApplicationContext()).getPlaylistList());

        if(Playlists.size() == 0)
        {
            Playlists.add(new MusicItem("No playlists", "", ((globalCommunication)getActivity().getApplicationContext()).addImage(AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.neonmusicicon)), false, false, "", 0));
        }

        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        myRvAdapter = new MyRvAdapter(Playlists);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);


        //Add data to the Special recycler view
        manageSpecial(view);

        //Create information popup message
        ImageView information = view.findViewById(R.id.information);
        information.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));
                popupMessage();
            }
        });


        //Add data to the tracks recycler view
        recyclerView = view.findViewById(R.id.TracksRecyclerView);
        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        myRvAdapter = new MyRvAdapter(musicItems);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);

        //Syncing tracks recycler view
        Handler mHandler = new Handler();
        new Thread(new Runnable() {

            @Override
            public void run() {
                recyclerView = view.findViewById(R.id.TracksRecyclerView);
                try{
                    if(getActivity() != null)
                    {
                        recyclerView.getAdapter().notifyDataSetChanged();
                        if(Special.size() <= 1)
                        {
                            manageSpecial(view);
                        }
                        if((((globalCommunication)getActivity().getApplicationContext()).getPlaylistList()).size() != 0){
                            Playlists.clear();
                            Playlists.addAll(((globalCommunication)getActivity().getApplicationContext()).getPlaylistList());
                            recyclerView = (view.findViewById(R.id.PlaylistsRecyclerView));
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                        if(((globalCommunication)getActivity().getApplicationContext()).isFinishedSyncing()){
                            return;
                        }
                    }
                }
                catch (Throwable e){

                }

                mHandler.postDelayed(this, 2000);
            }
        }).start();


        return view;
    }

    private void googleImageListener(View view){
        //Adding listener to google client image for log out and sync
        ImageView googleClientImage = view.findViewById(R.id.googleImage);
        googleClientImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getContext(), requireView().findViewById(R.id.googleImage), Gravity.CENTER, R.style.PopupTheme, R.style.PopupTheme);
                MenuInflater menuInflater = popupMenu.getMenuInflater();

                menuInflater.inflate(R.menu.verticalpopupmenu, popupMenu.getMenu());

                //Set the sync item to the current state
                popupMenu.getMenu().findItem(R.id.sync).setChecked(((globalCommunication)getActivity().getApplicationContext()).isSyncActivated());

                popupMenu.show();

                popupMenu.setOnMenuItemClickListener(item -> {
                    if(!item.isCheckable()){
                        if(!isOnline(getContext()))
                        {
                            Toast.makeText(getContext(), "Can't sign out. There is no internet connection", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        else{
                            signOut();
                            Intent intent = new Intent(getActivity(),Login.class);
                            startActivity(intent);
                        }
                        //Stop playing service
                        if(((globalCommunication)getActivity().getApplicationContext()).isSyncActivated()){
                            (getActivity().getApplicationContext()).stopService(new Intent( getActivity(), backgroundSync.class ) );
                        }
                    }
                    else{
                        //Change the state of sync
                        item.setChecked(!((globalCommunication)getActivity().getApplicationContext()).isSyncActivated());
                        ((globalCommunication)getActivity().getApplicationContext()).setSyncActivated(!((globalCommunication)getActivity().getApplicationContext()).isSyncActivated());

                        //If the user unchecked the sync button stop the background service
                        if(!item.isChecked()){
                            (getActivity().getApplicationContext()).stopService(new Intent( getActivity(), backgroundSync.class ) );
                        }
                    }
                    return true;

                });


            }
        });
    }
    private void setGoogleProfilePicture(String imageUrl, View view){
        ImageView googleClientImage = view.findViewById(R.id.googleImage);
        if(imageUrl != null){
            Glide.with(this).load(imageUrl).into(googleClientImage);
        }
    }

    private void popupMessage(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setMessage("The Special section is built with random items from your library!\n\n\n1. A random Artist\n2. A random song\n3. A random Playlist\n4,5. 2 more random songs");
        alertDialogBuilder.setTitle("Special description");
        alertDialogBuilder.setNegativeButton("ok", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    class MyRvAdapter extends RecyclerView.Adapter<MyRvAdapter.MyHolder>
    {


        ArrayList<MusicItem> musicItemArrayList;
        boolean isVeritcal = true;
        boolean isBig = false;

        public MyRvAdapter(ArrayList<MusicItem> musicItemArrayList){
            this.musicItemArrayList = musicItemArrayList;
            if(musicItemArrayList.size() != 0)
            {
                isVeritcal = musicItemArrayList.get(0).isVertical();
                isBig = musicItemArrayList.get(0).isBig();
            }
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            if(isVeritcal) { //MusicItem item
                view = LayoutInflater.from(getActivity()).inflate(R.layout.smallverticalscrollviewtrackitem, parent, false);
            }
            else if(isBig) { //Special item
                view = LayoutInflater.from(getActivity()).inflate(R.layout.bighorozobtialscrollviewitem, parent, false);
            }
            else { //Playlist item
                view = LayoutInflater.from(getActivity()).inflate(R.layout.horozontialscrollviewitem, parent, false);
            }
            return new MyHolder(view);
        }


        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {

            try{
                holder.title.setText(musicItemArrayList.get(position).getTitle());
                if(isVeritcal){ //Play track
                    holder.image.setImageDrawable(((globalCommunication)getActivity().getApplicationContext()).getImage(musicItemArrayList.get(position).getImageId()));

                    holder.subTitle.setText(musicItemArrayList.get(position).getSubTitle());

                    holder.plus.setOnClickListener(view->{
                        view.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));

                        PopupMenu popupMenu = new PopupMenu(getContext(), holder.plus, Gravity.CENTER, R.style.PopupTheme, R.style.PopupTheme);
                        MenuInflater menuInflater = popupMenu.getMenuInflater();

                        popupMenu.getMenu().add("Create Playlist");

                        for(MusicItem playlist : ((globalCommunication)getActivity().getApplicationContext()).getPlaylistList()){
                            popupMenu.getMenu().add(playlist.getTitle());
                        }

                        popupMenu.show();

                        popupMenu.setOnMenuItemClickListener(item -> {
                            playlistName = item.getTitle().toString();
                            if(item.getTitle().equals("Create Playlist")){

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Playlist Name:");

                                final EditText input = new EditText(getContext());
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                builder.setView(input);


                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playlistName = input.getText().toString();
                                        ((globalCommunication)getActivity().getApplicationContext()).createPlaylist(playlistName);
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

                            ((globalCommunication)getActivity().getApplicationContext()).addToPlaylist(musicItemArrayList.get(position), playlistName);

                            return true;
                        });
                    });

                    holder.itemView.setOnClickListener(view ->{
                        view.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));

                        ((globalCommunication)getActivity().getApplicationContext()).setPlaylistShuffle(false);
                        ArrayList<MusicItem> currentPlayingFullTrackList = new ArrayList<>();
                        currentPlayingFullTrackList.addAll(musicItemArrayList);
                        ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlayingList(currentPlayingFullTrackList);
                        ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlaying(((globalCommunication)getActivity().getApplicationContext()).getTrackPositionFromName(musicItemArrayList.get(position)));
                        try {
                            ((globalCommunication)getActivity().getApplicationContext()).load(musicItemArrayList.get(position).getPath());
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Problem loading track", Toast.LENGTH_SHORT).show();
                        }
                        ((globalCommunication)getActivity().getApplicationContext()).play();


                    });
                }
                else
                {
                    if(isBig){
                        holder.image.setImageDrawable(((globalCommunication)getActivity().getApplicationContext()).getImage(musicItemArrayList.get(position).getImageId()));
                        holder.subTitle.setText(musicItemArrayList.get(position).getSubTitle()); //In case there is a big playlist

                        //Check if special item is a MusicItem or a playlist
                        if(!(musicItemArrayList.get(position).getSubTitle().equals("Artist") || musicItemArrayList.get(position).getSubTitle().equals("Playlist") || musicItemArrayList.get(position).getSubTitle().equals(""))){
                            holder.itemView.setOnClickListener(view ->{
                                view.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));

                                ((globalCommunication)getActivity().getApplicationContext()).setPlaylistShuffle(false);
                                ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlayingList(musicItemArrayList);
                                ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlaying(((globalCommunication)getActivity().getApplicationContext()).getTrackPositionFromName(musicItemArrayList.get(position)));
                                try {
                                    ((globalCommunication)getActivity().getApplicationContext()).load(musicItemArrayList.get(position).getPath());
                                } catch (IOException e) {
                                    Toast.makeText(getContext(), "Problem loading track", Toast.LENGTH_SHORT).show();
                                }
                                ((globalCommunication)getActivity().getApplicationContext()).play();
                            });
                            return;
                        }
                    }
                    //Open playlist if not empty
                    if(!musicItemArrayList.get(position).getSubTitle().equals("")) {
                        holder.itemView.setOnClickListener(view -> {
                            view.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));

                            Intent playlistIntent = new Intent(getActivity(), playlist.class);
                            playlistIntent.putExtra("musicItemList", musicItems);
                            playlistIntent.putExtra("playlistItem", musicItemArrayList.get(position));
                            startActivity(playlistIntent);
                        });
                    }
                }
            }
            catch (Throwable e){

            }
        }


        @Override
        public int getItemCount() {
            return musicItemArrayList.size();
        }



        class MyHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subTitle;
            ImageView image;
            ImageView plus;

            public MyHolder(@NonNull View itemView) {
                super(itemView);
                try{
                    this.title = itemView.findViewById(R.id.title);
                    if(isVeritcal || isBig){
                        this.subTitle = itemView.findViewById(R.id.subTitle);
                        this.image = itemView.findViewById(R.id.image);

                        if(isVeritcal){
                            this.plus = itemView.findViewById(R.id.add);
                        }
                    }
                }
                catch (Throwable e){

                }
            }
        }
    }

    private void manageSpecial(View view){
        recyclerView = view.findViewById(R.id.SpecialRecyclerView);
        Special = new ArrayList<>();

        if(musicItems.size() <= 1)
        {
            Special.add(new MusicItem("Not enough tracks", "", ((globalCommunication)getActivity().getApplicationContext()).addImage(AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.neonmusicicon)), true, false, "", 0));
        }
        else
        {
            int randomArtist = new Random().nextInt(musicItems.size());
            int randomPlaylist = new Random().nextInt(Playlists.size());
            int randomTrack = new Random().nextInt(musicItems.size());
            int randomTrackSecond = new Random().nextInt(musicItems.size());
            int randomTrackThird = new Random().nextInt(musicItems.size());

            while((randomTrack == randomTrackSecond || randomTrack == randomTrackThird || randomTrackSecond == randomTrackThird ) && musicItems.size() > 3){
                randomTrack = new Random().nextInt(musicItems.size());
                randomTrackSecond = new Random().nextInt(musicItems.size());
                randomTrackThird = new Random().nextInt(musicItems.size());
            }

            Special.add(new MusicItem(musicItems.get(randomArtist).getSubTitle(), "Artist", musicItems.get(randomArtist).getImageId(), true, false, "", 0));
            Special.add(musicItems.get(randomTrack));
            Special.add(Playlists.get(randomPlaylist));
            Special.add(musicItems.get(randomTrackSecond));
            Special.add(musicItems.get(randomTrackThird));
        }


        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        myRvAdapter = new MyRvAdapter(Special);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);
    }
    private void signOut() {
        gsc.signOut();
    }


}