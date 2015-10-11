package net.nonylene.photolinkviewer.controller

import com.squareup.okhttp.Request
import net.nonylene.photolinkviewer.tool.OkHttpManager

class RedirectUrlController(private val callback: com.squareup.okhttp.Callback) {
    fun getRedirect(url : String) {
        OkHttpManager.okHttpClient.newCall(Request.Builder().url(url).head().build()).enqueue(callback)
    }
}
