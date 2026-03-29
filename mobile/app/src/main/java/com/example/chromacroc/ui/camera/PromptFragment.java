package com.example.chromacroc.ui.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.example.chromacroc.R;

import java.io.File;

public class PromptFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "photo_path";

    private ImageView   capturedImage;
    private ImageButton btnSend;
    private EditText    etAskAnything;
    private File        photoFile;

    public static PromptFragment newInstance(String photoPath) {
        PromptFragment fragment = new PromptFragment();
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
        btnSend       = view.findViewById(R.id.btnSend);
        etAskAnything = view.findViewById(R.id.etAskAnything);

        if (getArguments() != null) {
            String photoPath = getArguments().getString(ARG_PHOTO_PATH);
            photoFile = new File(photoPath);
            capturedImage.setImageBitmap(fixRotation(BitmapFactory.decodeFile(photoPath), photoPath));
        }

        btnSend.setOnClickListener(v ->
                navigateToResponse(etAskAnything.getText().toString().trim()));

        etAskAnything.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                navigateToResponse(etAskAnything.getText().toString().trim());
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnWhatColor)
                .setOnClickListener(v -> navigateToResponse("What color is this?"));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etAskAnything.postDelayed(() -> {
            etAskAnything.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etAskAnything, android.view.inputmethod.InputMethodManager.SHOW_FORCED);
        }, 300);
    }

    private Bitmap fixRotation(Bitmap bitmap, String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            float angle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90)  angle = 90f;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) angle = 180f;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) angle = 270f;
            if (angle == 0) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }

    private void navigateToResponse(String question) {
        if (photoFile == null) return;
        ResponseFragment next = ResponseFragment.newInstance(
                photoFile.getAbsolutePath(), question);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, next)
                .addToBackStack(null)
                .commit();
    }
}