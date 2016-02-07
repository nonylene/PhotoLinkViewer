package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.preference.PreferenceManager
import android.util.Base64
import net.nonylene.photolinkviewer.core.PhotoLinkViewer
import java.util.*
import javax.crypto.spec.SecretKeySpec

object PLVUtils {

    fun refreshTwitterTokens(database: SQLiteDatabase) {
        val map = MyAsyncTwitter.getAccountsTokenList(database)
        refreshTwitterTokens(map)
    }

    fun refreshTwitterTokens(context: Context) {
        val map = MyAsyncTwitter.getAccountsTokenList(context)
        refreshTwitterTokens(map)
    }

    private fun refreshTwitterTokens(map: LinkedHashMap<String, PhotoLinkViewer.TwitterToken>) {
        PhotoLinkViewer.twitterTokenMap.clear()
        PhotoLinkViewer.twitterTokenMap.putAll(map)
    }

    /**
     * @param instagramEnabled if instagram Enabled preference set manually, set value.
     * if use default value, set null.
     */
    fun refreshInstagramToken(context: Context, instagramEnabled: Boolean? = null) {
        val preferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE)

        if (instagramEnabled ?: PreferenceManager.getDefaultSharedPreferences(context).isInstagramEnabled()
                && preferences.getBoolean("instagram_authorized", false)) {
            val keyByte = Base64.decode(preferences.getString("instagram_key", null), Base64.DEFAULT)
            val tokenByte = Base64.decode(preferences.getString("instagram_token", null), Base64.DEFAULT)
            val token = Encryption.decrypt(tokenByte, SecretKeySpec(keyByte, 0, keyByte.size, "AES"))
            PhotoLinkViewer.instagramToken = token
        } else {
            PhotoLinkViewer.instagramToken = null
        }
    }
}
