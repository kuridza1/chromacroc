package com.example.chromacroc.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.chromacroc.R;

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

        // Back
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        // Svi tipovi vode na CameraFragment
        View.OnClickListener toCameraListener = v ->
                Navigation.findNavController(v).navigate(R.id.action_diagnosisSelectFragment_to_cameraFragment);

        view.findViewById(R.id.btn_green_type).setOnClickListener(toCameraListener);
        view.findViewById(R.id.btn_red_type).setOnClickListener(toCameraListener);
        view.findViewById(R.id.btn_blue_type).setOnClickListener(toCameraListener);
        view.findViewById(R.id.btn_total).setOnClickListener(toCameraListener);

        return view;
    }
}