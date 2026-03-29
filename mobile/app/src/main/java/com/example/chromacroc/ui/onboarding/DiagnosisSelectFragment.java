package com.example.chromacroc.ui.onboarding;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;
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

        // Postavi touch animaciju na sva 4 dugmeta
        int[] buttonIds = {
                R.id.btn_red_type,
                R.id.btn_green_type,
                R.id.btn_blue_type,
                R.id.btn_total
        };
        for (int id : buttonIds) {
            addTouchEffect(view.findViewById(id));
        }

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

    private void addTouchEffect(CardView card) {
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((CardView) v).setCardBackgroundColor(Color.parseColor("#E0E0E0"));
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ((CardView) v).setCardBackgroundColor(Color.parseColor("#F2F2F2"));
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }
}