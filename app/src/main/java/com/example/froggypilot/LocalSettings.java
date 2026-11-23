package com.example.froggypilot;
import android.content.Context;
import android.content.SharedPreferences;
/** Offline app preferences; no credentials, API keys, or network access. */
final class LocalSettings {
    private final SharedPreferences prefs;
    LocalSettings(Context context){
        prefs=context.getSharedPreferences("froggypilot_settings",Context.MODE_PRIVATE);
    }
    String getText(String key,String fallback){return prefs.getString(key,fallback);}
    void putText(String key,String val){prefs.edit().putString(key,val).apply();}
}
