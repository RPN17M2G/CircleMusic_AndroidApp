package com.example.circleMusic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


public class search extends Fragment {

    private SearchView searchView;
    private  RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private search.MyRvAdapter myRvAdapter;
    private ArrayList<MusicItem> musicItemList;
    private ArrayList<MusicItem> musicItemListCpy;
    private String playlistName = "";


    public search() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static search newInstance(String param1, String param2) {
        search fragment = new search();


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
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        musicItemList = ((globalCommunication)getActivity().getApplicationContext()).getTrackList();
        musicItemListCpy = new ArrayList<>();
        musicItemListCpy.addAll(musicItemList);

        //Initalizing the search bar
        searchView = view.findViewById(R.id.searchView);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) { //Filter and show the matching items on the list
                musicItemListCpy.clear();
                musicItemListCpy.addAll(filterList(newText));
                myRvAdapter.notifyDataSetChanged();
                return true;
            }
        });


        recyclerView = view.findViewById(R.id.recyclerViewSearch);



        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        myRvAdapter = new search.MyRvAdapter(musicItemListCpy);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(myRvAdapter);

        return view;
    }

    private ArrayList<MusicItem> filterList(String text) {
        ArrayList<MusicItem> filteredList = new ArrayList<>();
        for(MusicItem musicItem : musicItemList){  //Check if the MusicItem is matching the search
            if(musicItem.getTitle().toLowerCase().contains(text.toLowerCase()) || musicItem.getSubTitle().toLowerCase().contains(text.toLowerCase())){
                filteredList.add(musicItem);
            }
            else if(text.equals(""))
            {
                filteredList.add(musicItem);
            }
        }

        return filteredList;
    }


    class MyRvAdapter extends RecyclerView.Adapter<search.MyRvAdapter.MyHolder>
    {
        ArrayList<MusicItem> musicItemArrayList;

        public MyRvAdapter(ArrayList<MusicItem> musicItemArrayList){
            this.musicItemArrayList = musicItemArrayList;

        }


        @NonNull
        @Override
        public search.MyRvAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.verticalscrollviewtrackitem, parent, false);

            return new search.MyRvAdapter.MyHolder(view);
        }


        @Override
        public void onBindViewHolder(@NonNull search.MyRvAdapter.MyHolder holder, int position) {
            if(position >= 0 && position < musicItemArrayList.size()){
                holder.title.setText(musicItemArrayList.get(position).getTitle());
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

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            playlistName = item.getTitle().toString();
                            if(playlistName.equals("Create Playlist")){

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

                            Log.d("DEBUG", playlistName);
                            ((globalCommunication)getActivity().getApplicationContext()).addToPlaylist(musicItemArrayList.get(position), playlistName);

                            return true;
                        }
                    });
                });

                holder.itemView.setOnClickListener(view->{
                    view.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.track_click));

                    ((globalCommunication)getActivity().getApplicationContext()).setPlaylistShuffle(false);
                    ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlayingList(musicItemArrayList);
                    ((globalCommunication)getActivity().getApplicationContext()).setCurrentPlaying(((globalCommunication)getActivity().getApplicationContext()).getTrackPositionFromName(musicItemArrayList.get(position)));
                    Log.d("DEBUG", String.valueOf(((globalCommunication)getActivity().getApplicationContext()).getTrackPositionFromName(musicItemArrayList.get(position))));
                    try {
                        ((globalCommunication)getActivity().getApplicationContext()).load(musicItemArrayList.get(position).getPath());
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Problem loading track", Toast.LENGTH_SHORT).show();
                    }
                    ((globalCommunication)getActivity().getApplicationContext()).play();

                    });
            }
        }


        @Override
        public int getItemCount() {
            return musicItemArrayList.size();
        }

        public class MyHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subTitle;
            ImageView image;
            ImageView plus;


            public TextView getTitle() {
                return title;
            }

            public TextView getSubTitle() {
                return subTitle;
            }



            public MyHolder(@NonNull View itemView) {
                super(itemView);
                this.title = itemView.findViewById(R.id.title);
                this.image = itemView.findViewById(R.id.image);
                this.subTitle = itemView.findViewById(R.id.subTitle);
                this.plus = itemView.findViewById(R.id.add);
            }


        }
    }


}