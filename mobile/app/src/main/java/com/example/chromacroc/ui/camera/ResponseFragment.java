package com.example.chromacroc.ui.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.example.chromacroc.R;
import com.example.chromacroc.model.ColorBlindnessType;
import com.example.chromacroc.services.AgentApiClient;
import com.example.chromacroc.services.UserPreferences;

import java.io.File;

public class ResponseFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "photo_path";
    private static final String ARG_QUESTION   = "question";

    private ImageView    bgImage;
    private LinearLayout llThinkingContainer;
    private ScrollView   scrollView;
    private EditText     etAskMore;
    private ImageButton  btnAskMore;

    private File               photoFile;
    private ColorBlindnessType colorBlindnessType;

    // Aktivna kartica koja prima chunk-ove
    private View     activeCard     = null;
    private TextView activeCardBody = null;

    // Thinking kartica i njen animator
    private View    thinkingCard    = null;
    private Handler thinkingHandler = new Handler(Looper.getMainLooper());
    private Runnable thinkingRunnable;
    private int thinkingDotCount   = 0;

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    public static ResponseFragment newInstance(String photoPath, String question) {
        ResponseFragment f = new ResponseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        args.putString(ARG_QUESTION, question);
        f.setArguments(args);
        return f;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_response, container, false);

        bgImage             = view.findViewById(R.id.bgImage);
        llThinkingContainer = view.findViewById(R.id.llThinkingContainer);
        scrollView          = view.findViewById(R.id.scrollView);
        etAskMore           = view.findViewById(R.id.etAskMore);
        btnAskMore          = view.findViewById(R.id.btnAskMore);

        colorBlindnessType = new UserPreferences(requireContext()).getColorBlindnessType();

        if (getArguments() != null) {
            String photoPath = getArguments().getString(ARG_PHOTO_PATH);
            photoFile = new File(photoPath);

            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            if (bitmap != null) {
                bitmap = fixRotation(bitmap, photoPath);
                bgImage.setImageBitmap(fastBlur(bitmap, 0.5f));
            }

            String question = getArguments().getString(ARG_QUESTION, "");
            askAgent(question);
        }

        btnAskMore.setOnClickListener(v -> {
            String q = etAskMore.getText().toString().trim();
            if (!q.isEmpty()) {
                etAskMore.setText("");
                askAgent(q);
            }
        });

        etAskMore.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String q = etAskMore.getText().toString().trim();
                if (!q.isEmpty()) {
                    etAskMore.setText("");
                    askAgent(q);
                }
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnBackToCamera)
                .setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager().popBackStack());

        // Fokus i tastatura
        etAskMore.requestFocus();
        etAskMore.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etAskMore, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopThinking();
    }

    // -----------------------------------------------------------------------
    // Thinking animacija
    // -----------------------------------------------------------------------

    private void startThinking() {
        if (getActivity() == null) return;
        thinkingCard = addCard("Thinking", "");
        TextView body = thinkingCard.findViewById(R.id.tvStepBody);

        thinkingDotCount = 0;
        thinkingRunnable = new Runnable() {
            @Override
            public void run() {
                if (thinkingCard == null || getActivity() == null) return;
                thinkingDotCount = (thinkingDotCount % 3) + 1;
                String dots = new String(new char[thinkingDotCount]).replace("\0", ".");
                getActivity().runOnUiThread(() -> body.setText(dots));
                thinkingHandler.postDelayed(this, 500);
            }
        };
        thinkingHandler.post(thinkingRunnable);
        scrollToBottom();
    }

    private void stopThinking() {
        thinkingHandler.removeCallbacks(thinkingRunnable != null ? thinkingRunnable : () -> {});
        if (thinkingCard != null && llThinkingContainer != null) {
            int idx = llThinkingContainer.indexOfChild(thinkingCard);
            if (idx > 0) {
                llThinkingContainer.removeViewAt(idx - 1);
            }
            llThinkingContainer.removeView(thinkingCard);
            thinkingCard = null;
        }
    }

    // -----------------------------------------------------------------------
    // Agent poziv — SSE stream
    // -----------------------------------------------------------------------

    private void askAgent(String question) {
        if (photoFile == null || !photoFile.exists()) return;

        btnAskMore.setEnabled(false);
        etAskMore.setEnabled(false);

        getActivity().runOnUiThread(this::startThinking);

        AgentApiClient.askStreaming(
                photoFile,
                question,
                colorBlindnessType,
                new AgentApiClient.StreamCallback() {

                    @Override
                    public void onAgentStart(String agentName) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            stopThinking();
                            activeCard     = addCard(formatAgentName(agentName), "");
                            activeCardBody = activeCard.findViewById(R.id.tvStepBody);
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onChunk(String agentName, String textChunk) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (activeCardBody == null) {
                                stopThinking();
                                activeCard     = addCard(formatAgentName(agentName), "");
                                activeCardBody = activeCard.findViewById(R.id.tvStepBody);
                            }

                            String current = activeCardBody.getText().toString();
                            String updated = current + textChunk;

                            int blankIdx = updated.indexOf("\n\n");
                            if (blankIdx == -1) {
                                TextView title = activeCard.findViewById(R.id.tvStepTitle);
                                title.setText(formatAgentName(agentName) + ": " + updated.replace("\n", ""));
                            } else {
                                activeCardBody.setText(updated.substring(blankIdx + 2));
                            }

                            activeCard.setTag(updated);
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onComplete() {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            stopThinking();

                            if (activeCard != null) {
                                String fullText = activeCard.getTag() != null
                                        ? activeCard.getTag().toString() : "";
                                activeCard.findViewById(R.id.tvStepTitle).setVisibility(View.GONE);
                                activeCardBody.setText(fullText);
                                activeCardBody.setTextColor(0xFF111111);
                                activeCardBody.setTextSize(15f);
                            }
                            activeCard     = null;
                            activeCardBody = null;
                            btnAskMore.setEnabled(true);
                            etAskMore.setEnabled(true);
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            stopThinking();
                            addCard("Error", error);
                            btnAskMore.setEnabled(true);
                            etAskMore.setEnabled(true);
                        });
                    }
                }
        );
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private View addCard(String title, String body) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View card = inflater.inflate(R.layout.item_thinking_step, llThinkingContainer, false);

        ((TextView) card.findViewById(R.id.tvStepTitle)).setText(title);
        ((TextView) card.findViewById(R.id.tvStepBody)).setText(body);

        if (llThinkingContainer.getChildCount() > 0) {
            View divider = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, 16, 0, 16);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFFEEEEEE);
            llThinkingContainer.addView(divider);
        }

        llThinkingContainer.addView(card);
        return card;
    }

    private String formatAgentName(String raw) {
        if (raw == null || raw.isEmpty()) return "Thinking…";
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                        .append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    // -----------------------------------------------------------------------
    // Rotacija (EXIF fix)
    // -----------------------------------------------------------------------

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
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }

    // -----------------------------------------------------------------------
    // Blur (RenderScript — glatki Gaussian)
    // -----------------------------------------------------------------------

    private Bitmap fastBlur(Bitmap src, float scale) {
        int w = Math.max(1, Math.round(src.getWidth()  * scale));
        int h = Math.max(1, Math.round(src.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);

        RenderScript rs = RenderScript.create(requireContext());
        Allocation input  = Allocation.createFromBitmap(rs, small);
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blur.setRadius(25f);
        blur.setInput(input);
        blur.forEach(output);
        output.copyTo(small);
        rs.destroy();

        return Bitmap.createScaledBitmap(small, src.getWidth(), src.getHeight(), true);
    }
}