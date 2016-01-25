package net.nonylene.photolinkviewer.tool

import android.content.SharedPreferences

private val WIFI_SWITCH_KEY = "wifi_switch"
private val INSTAGRAM_SWITCH_KEY = "instagram_api"

fun SharedPreferences.getWifiEnabled(defaultValue: Boolean = false): Boolean {
    return getBoolean(WIFI_SWITCH_KEY, defaultValue)
}

fun SharedPreferences.Editor.putIsWifiEnabled(value: Boolean): SharedPreferences.Editor {
    return putBoolean(WIFI_SWITCH_KEY, value)
}

fun SharedPreferences.getInstagramEnabled(defaultValue: Boolean = false): Boolean {
    return getBoolean(INSTAGRAM_SWITCH_KEY, defaultValue)
}

fun SharedPreferences.Editor.putIsInstagramEnabled(value: Boolean): SharedPreferences.Editor {
    return putBoolean(INSTAGRAM_SWITCH_KEY, value)
}

fun SharedPreferences.getQuality(siteName: String, isWifi: Boolean, defaultValue: String = "large"): String {
    return getString("${siteName}_quality_" + if (isWifi) "wifi" else "3g", defaultValue)
}

fun SharedPreferences.Editor.putQuality(value: String, siteName: String, isWifi: Boolean): SharedPreferences.Editor {
    return putString("${siteName}_quality_" + if (isWifi) "wifi" else "3g", value)
}

