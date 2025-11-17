package com.example.smartwallet;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPrefs {

    private static final String PREF_NAME = "user_data";
    private static final String KEY_FULL_NAME = "full_name";

    public static void saveFullName(Context context, String fullName) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    public static String getFullName(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_FULL_NAME, "User");
    }
}
