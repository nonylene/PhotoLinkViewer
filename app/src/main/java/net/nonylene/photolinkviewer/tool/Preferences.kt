package net.nonylene.photolinkviewer.tool

import android.content.SharedPreferences

private val INSTAGRAM_SWITCH_KEY = "instagram_api"
private val TWITTER_OAUTHED_KEY = "authorized"
private val TWITTER_DEFAULT_SCREEN_KEY = "screen_name"
private val SHOWN_INITIAL_FAQ_SNACK_BAR = "initial_faq_snack_bar"

fun SharedPreferences.isInstagramEnabled(defaultValue: Boolean = false): Boolean {
    return getBoolean(INSTAGRAM_SWITCH_KEY, defaultValue)
}

fun SharedPreferences.Editor.putIsInstagramEnabled(value: Boolean): SharedPreferences.Editor {
    return putBoolean(INSTAGRAM_SWITCH_KEY, value)
}

fun SharedPreferences.isTwitterOAuthed(defaultValue: Boolean = false): Boolean {
    return getBoolean(TWITTER_OAUTHED_KEY, defaultValue)
}

fun SharedPreferences.Editor.putIsTwitterOAuthed(value: Boolean): SharedPreferences.Editor {
    return putBoolean(TWITTER_OAUTHED_KEY, value)
}

fun SharedPreferences.getDefaultTwitterScreenName(defaultValue: String? = null): String? {
    return getString(TWITTER_DEFAULT_SCREEN_KEY, defaultValue)
}

fun SharedPreferences.Editor.putDefaultTwitterScreenName(value: String?): SharedPreferences.Editor {
    return putString(TWITTER_DEFAULT_SCREEN_KEY, value)
}

fun SharedPreferences.shownInitialFaqSnackBar(defaultValue: Boolean = false): Boolean{
    return getBoolean(SHOWN_INITIAL_FAQ_SNACK_BAR, defaultValue)
}

fun SharedPreferences.Editor.putShownInitialFaqSnackBar(value: Boolean): SharedPreferences.Editor {
    return putBoolean(SHOWN_INITIAL_FAQ_SNACK_BAR, value)
}
