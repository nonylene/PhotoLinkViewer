package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.nonylene.photolinkviewer.core.PhotoLinkViewer
import java.util.*

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
}
