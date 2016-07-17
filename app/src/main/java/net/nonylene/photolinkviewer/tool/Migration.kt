package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.preference.PreferenceManager


fun migrate52(context: Context) {
    val defaultPreference = PreferenceManager.getDefaultSharedPreferences(context)
    if (!defaultPreference.getBoolean("initialized52", false)) {
        context.getSharedPreferences("preference", Context.MODE_PRIVATE).edit()
                .putString("instagram_token", null)
                .putString("instagram_key", null)
                .putString("instagram_id", null)
                .putString("instagram_username", null)
                .putString("instagram_icon", null)
                .putBoolean("instagram_authorized", false)
                .apply()
        defaultPreference.edit()
                .putIsInstagramEnabled(false)
                .putBoolean("initialized52", true)
                .apply()
    }
}

