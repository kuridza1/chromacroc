package com.example.chromacroc.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.chromacroc.R;

public class WelcomeFragment extends Fragment {

    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        Button btnStart = view.findViewById(R.id.btn_get_started);
        btnStart.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_welcomeFragment_to_diagnosisChoiceFragment)
        );

        return view;
    }
}