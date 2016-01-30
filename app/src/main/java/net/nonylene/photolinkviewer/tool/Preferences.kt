package net.nonylene.photolinkviewer.tool

import android.content.SharedPreferences

private val INSTAGRAM_SWITCH_KEY = "instagram_api"

fun SharedPreferences.getInstagramEnabled(defaultValue: Boolean = false): Boolean {
    return getBoolean(INSTAGRAM_SWITCH_KEY, defaultValue)
}

fun SharedPreferences.Editor.putIsInstagramEnabled(value: Boolean): SharedPreferences.Editor {
    return putBoolean(INSTAGRAM_SWITCH_KEY, value)
}
