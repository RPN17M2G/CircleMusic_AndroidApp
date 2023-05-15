package com.example.circleMusic;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class artists extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<MusicItem> musicItems;
    private ArrayList<MusicItem> Artists;
    private LinearLayoutManager linearLayoutManager;
    private artists.MyRvAdapter myRvAdapter;


    public artists() {
        // Required empty public constructor
    }


    public static artists newInstance(String param1, String param2) {
        artists fragment = new artists();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_artists, container, false);

        musicItems = ((globalCommunication)getActivity().getApplicationContext()).getTrackList();

        recyclerView = view.findViewById(R.id.TracksRecyclerView);

        Artists = new ArrayList<>();

        for(MusicItem musicItem : musicItems)
        {
            if(!contains(Artists, musicItem.getSubTitle()))
            {
                Artists.add(new MusicItem(musicItem.getSubTitle(), "Artist", musicItem.getImageId(), false, false, "", 0));
            }
        }

        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        myRvAdapter = new artists.MyRvAdapter(Artists);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);

        return view;
    }

    private boolean contains(ArrayList<MusicItem> artists, String subTitle) {
        for(MusicItem musicItem : artists)
        {
            if(musicItem.getTitle().equals(subTitle)){
                return true;
            }
        }
        return false;
    }

    class MyRvAdapter extends RecyclerView.Adapter<artists.MyRvAdapter.MyHolder> {


        ArrayList<MusicItem> musicItemArrayList;

        public MyRvAdapter(ArrayList<MusicItem> musicItemArrayList) {
            this.musicItemArrayList = musicItemArrayList;
        }

        @NonNull
        @Override
        public artists.MyRvAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new artists.MyRvAdapter.MyHolder(LayoutInflater.from(getActivity()).inflate(R.layout.artistsverticalrecyclerviewitem, parent, false));
        }


        @Override
        public void onBindViewHolder(@NonNull artists.MyRvAdapter.MyHolder holder, int position) {
            holder.title.setText(musicItemArrayList.get(position).getTitle());
            holder.image.setImageDrawable(((globalCommunication)getActivity().getApplicationContext()).getImage(musicItemArrayList.get(position).getImageId()));
            holder.itemView.setOnClickListener(view ->{
                Intent playlistIntent = new Intent(getActivity(), playlist.class);
                playlistIntent.putExtra("musicItemList", musicItems);
                playlistIntent.putExtra("playlistItem", musicItemArrayList.get(position));
                startActivity(playlistIntent);
            });
        }


        @Override
        public int getItemCount() {

            return musicItemArrayList.size();
        }


        class MyHolder extends RecyclerView.ViewHolder {
            TextView title;
            ImageView image;

            public MyHolder(@NonNull View itemView) {
                super(itemView);
                this.title = itemView.findViewById(R.id.title);
                this.image = itemView.findViewById(R.id.image);
                }
            }
        }
    }

