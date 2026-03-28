package com.example.chromacroc.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.chromacroc.R;

public class DiagnosisChoiceFragment extends Fragment {

    public DiagnosisChoiceFragment() {}

    public static DiagnosisChoiceFragment newInstance() {
        return new DiagnosisChoiceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnosis_choice, container, false);

//        view.findViewById(R.id.btn_test_vision).setOnClickListener(v ->
//                Navigation.findNavController(v).navigate(R.id.action_diagnosisChoiceFragment_to_testVisionFragment)
//        );

        view.findViewById(R.id.btn_color_blind).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_diagnosisChoiceFragment_to_diagnosisSelectFragment)
        );

        return view;
    }
}