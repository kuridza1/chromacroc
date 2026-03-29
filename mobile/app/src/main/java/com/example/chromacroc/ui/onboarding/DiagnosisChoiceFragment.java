package com.example.chromacroc.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;
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

        CardView btnColorBlind = view.findViewById(R.id.btn_color_blind);

        btnColorBlind.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((CardView) v).setCardBackgroundColor(
                            android.graphics.Color.parseColor("#E0E0E0"));
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ((CardView) v).setCardBackgroundColor(
                            android.graphics.Color.parseColor("#F2F2F2"));
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });

        btnColorBlind.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_diagnosisChoiceFragment_to_diagnosisSelectFragment)
        );

        return view;
    }
}