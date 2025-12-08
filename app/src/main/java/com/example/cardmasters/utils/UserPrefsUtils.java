package com.example.cardmasters.utils;



import android.content.Context;
import android.content.SharedPreferences;

public class UserPrefsUtils {

    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USERNAME = "username";

    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";


    public static void saveUsername(Context context, String username) {

        getPrefs(context).edit().putString(KEY_USERNAME, username).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------
    // SAVE ALL USER DATA
    // -------------------------
    public static void saveUserData(Context context, String email, String password, String username) {
        getPrefs(context).edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    // -------------------------
    // GETTERS (ENCAPSULATION)
    // -------------------------
    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, null);
    }

    public static String getPassword(Context context) {
        return getPrefs(context).getString(KEY_PASSWORD, null);
    }

    public static String getUsername(Context context) {
        return getPrefs(context).getString(KEY_USERNAME, null);
    }

    // -------------------------
    // CLEAR USER DATA
    // -------------------------
    public static void clear(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
