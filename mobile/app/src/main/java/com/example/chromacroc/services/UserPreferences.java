package com.example.chromacroc.services;


import android.content.Context;
import android.content.SharedPreferences;

import com.example.chromacroc.model.ColorBlindnessType;

public class UserPreferences {

    private static final String PREFS_NAME = "chroma_croc_prefs";
    private static final String KEY_COLOR_BLINDNESS_TYPE = "color_blindness_type";

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveColorBlindnessType(ColorBlindnessType type) {
        prefs.edit().putString(KEY_COLOR_BLINDNESS_TYPE, type.name()).apply();
    }

    /** Returns null if the user hasn't selected a type yet. */
    public ColorBlindnessType getColorBlindnessType() {
        String saved = prefs.getString(KEY_COLOR_BLINDNESS_TYPE, null);
        if (saved == null) return null;
        try {
            return ColorBlindnessType.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean hasColorBlindnessType() {
        return getColorBlindnessType() != null;
    }

    public void clear() {
        prefs.edit().remove(KEY_COLOR_BLINDNESS_TYPE).apply();
    }
}