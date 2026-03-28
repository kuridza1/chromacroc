package com.example.chromacroc.ui.camera;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.chromacroc.R;

public class ResultFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "photo_path";

    public static ResultFragment newInstance(String photoPath) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        ImageView capturedImage = view.findViewById(R.id.capturedImage);
        Button btnWhatColor = view.findViewById(R.id.btnWhatColor);
        EditText etAskAnything = view.findViewById(R.id.etAskAnything);

        // Učitaj i prikaži sliku
        if (getArguments() != null) {
            String photoPath = getArguments().getString(ARG_PHOTO_PATH);
            capturedImage.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        // "What color is this?" dugme
        btnWhatColor.setOnClickListener(v -> {
            // TODO: Ovdje pozovi AI da odredi boju
            Toast.makeText(getContext(), "Analyzing color...", Toast.LENGTH_SHORT).show();
        });

        // "Ask anything" text field - pritisak Enter šalje pitanje
        etAskAnything.setOnEditorActionListener((v, actionId, event) -> {
            String question = etAskAnything.getText().toString();
            if (!question.isEmpty()) {
                // TODO: Pošalji pitanje AI-u
                Toast.makeText(getContext(), "Asked: " + question, Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        return view;
    }
}