package net.nonylene.photolinkviewer.controller

import android.content.Context
import com.squareup.okhttp.Request
import net.nonylene.photolinkviewer.tool.OkHttpManager

class RedirectUrlController(private val callback: com.squareup.okhttp.Callback, private val context: Context) {
    fun getRedirect(url : String) {
        OkHttpManager.getOkHttpClient(context).newCall(Request.Builder().url(url).get().build()).enqueue(callback)
    }
}
