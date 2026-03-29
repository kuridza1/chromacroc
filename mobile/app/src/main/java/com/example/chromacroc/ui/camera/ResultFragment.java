package com.example.chromacroc.ui.camera;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.chromacroc.R;
import com.example.chromacroc.services.UserPreferences;
import com.example.chromacroc.model.ColorBlindnessType;
import com.example.chromacroc.services.AgentApiClient;

import java.io.File;

public class ResultFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "photo_path";

    private ImageView capturedImage;
    private Button btnWhatColor;
    private EditText etAskAnything;
    private TextView tvResponse;
    private ProgressBar progressBar;
    private File photoFile;

    private ColorBlindnessType colorBlindnessType;

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

        capturedImage = view.findViewById(R.id.capturedImage);
        btnWhatColor  = view.findViewById(R.id.btnWhatColor);
        etAskAnything = view.findViewById(R.id.etAskAnything);
        tvResponse    = view.findViewById(R.id.tvResponse);
        progressBar   = view.findViewById(R.id.progressBar);

        // Učitaj sačuvani tip color blindness-a
        colorBlindnessType = new UserPreferences(requireContext()).getColorBlindnessType();

        // Učitaj i prikaži sliku
        if (getArguments() != null) {
            String photoPath = getArguments().getString(ARG_PHOTO_PATH);
            photoFile = new File(photoPath);
            capturedImage.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        btnWhatColor.setOnClickListener(v -> sendToAgent("What color is this?"));

        etAskAnything.setOnEditorActionListener((v, actionId, event) -> {
            String question = etAskAnything.getText().toString().trim();
            if (!question.isEmpty()) {
                sendToAgent(question);
                etAskAnything.setText("");
            }
            return true;
        });

        return view;
    }

    private void sendToAgent(String question) {
        if (photoFile == null || !photoFile.exists()) {
            tvResponse.setText("Error: Image file not found.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvResponse.setText("Thinking...");
        btnWhatColor.setEnabled(false);
        etAskAnything.setEnabled(false);

        AgentApiClient.ask(photoFile, question, colorBlindnessType, new AgentApiClient.Callback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResponse.setText(response);
                    btnWhatColor.setEnabled(true);
                    etAskAnything.setEnabled(true);
                });
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResponse.setText("Error: " + error);
                    btnWhatColor.setEnabled(true);
                    etAskAnything.setEnabled(true);
                });
            }
        });
    }
}