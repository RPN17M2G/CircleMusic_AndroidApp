package com.example.circleMusic;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.circleMusic.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link errorPage#newInstance} factory method to
 * create an instance of this fragment.
 */
public final class errorPage extends Fragment {



    public errorPage() {
        // Required empty public constructor
    }


    public static errorPage newInstance(String param1, String param2) {
        errorPage fragment = new errorPage();

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
        return inflater.inflate(R.layout.fragment_error_page, container, false);
    }
}