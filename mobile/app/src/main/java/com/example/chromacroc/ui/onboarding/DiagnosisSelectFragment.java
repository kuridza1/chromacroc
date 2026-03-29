package com.example.chromacroc.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.chromacroc.R;
import com.example.chromacroc.services.UserPreferences;
import com.example.chromacroc.model.ColorBlindnessType;

public class DiagnosisSelectFragment extends Fragment {

    public DiagnosisSelectFragment() {}

    public static DiagnosisSelectFragment newInstance() {
        return new DiagnosisSelectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnosis_select, container, false);
        UserPreferences prefs = new UserPreferences(requireContext());

        // Back
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        view.findViewById(R.id.btn_red_type).setOnClickListener(v -> {
            prefs.saveColorBlindnessType(ColorBlindnessType.PROTANOPIA);
            Navigation.findNavController(v).navigate(R.id.action_diagnosisSelectFragment_to_cameraFragment);
        });

        view.findViewById(R.id.btn_green_type).setOnClickListener(v -> {
            prefs.saveColorBlindnessType(ColorBlindnessType.DEUTERANOPIA);
            Navigation.findNavController(v).navigate(R.id.action_diagnosisSelectFragment_to_cameraFragment);
        });

        view.findViewById(R.id.btn_blue_type).setOnClickListener(v -> {
            prefs.saveColorBlindnessType(ColorBlindnessType.TRITANOPIA);
            Navigation.findNavController(v).navigate(R.id.action_diagnosisSelectFragment_to_cameraFragment);
        });

        view.findViewById(R.id.btn_total).setOnClickListener(v -> {
            prefs.saveColorBlindnessType(ColorBlindnessType.ACHROMATOPSIA);
            Navigation.findNavController(v).navigate(R.id.action_diagnosisSelectFragment_to_cameraFragment);
        });

        return view;
    }
}