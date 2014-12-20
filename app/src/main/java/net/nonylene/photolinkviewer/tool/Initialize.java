package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Initialize {

    public static void initialize19(Context context) {
        // ver 19 (quality setting)
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String old = preferences.getString("quality", "large");
        SharedPreferences.Editor editor = preferences.edit();
        switch (old) {
            case "original":
                editor.putString("flickr_quality_3g", "original");
                editor.putString("twitter_quality_3g", "original");
                editor.putString("twipple_quality_3g", "original");
                editor.putString("imgly_quality_3g", "full");
                editor.putString("instagram_quality_3g", "large");
                break;
            case "large":
                editor.putString("flickr_quality_3g", "large");
                editor.putString("twitter_quality_3g", "large");
                editor.putString("twipple_quality_3g", "large");
                editor.putString("imgly_quality_3g", "large");
                editor.putString("instagram_quality_3g", "large");
                break;

            case "medium":
                editor.putString("flickr_quality_3g", "medium");
                editor.putString("twitter_quality_3g", "medium");
                editor.putString("twipple_quality_3g", "thumb");
                editor.putString("imgly_quality_3g", "medium");
                editor.putString("instagram_quality_3g", "medium");
                break;

        }
        editor.remove("quality");
        editor.putBoolean("initialized19",true);
        editor.apply();
    }
}
