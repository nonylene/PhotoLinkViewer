package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.net.http.HttpResponseCache
import android.preference.PreferenceManager

object Initialize {

    public fun initialize39(context: Context) {
        // ver 39 (clear old cache)
        HttpResponseCache.getInstalled()?.delete()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean("initialized39", true)
            .apply()
    }
}
