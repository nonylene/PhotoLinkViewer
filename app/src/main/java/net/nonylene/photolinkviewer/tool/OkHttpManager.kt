package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.util.Log
import com.squareup.okhttp.Cache
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.OkHttpClient
import java.io.File
import java.io.IOException

object OkHttpManager {
    private val okHttpClient : OkHttpClient = OkHttpClient()
    private var cache : Cache? = null

    public fun getOkHttpClient(context: Context) : OkHttpClient {
        if (cache == null){
            //enable cache
            try {
                val cacheDir = File(context.applicationContext.cacheDir, "okhttp")
                cache = Cache(cacheDir, 3 * 1024 * 1024) // 3 MB
            } catch (e: IOException) {
                Log.d("cache", "HTTP response cache installation failed")
            }
            okHttpClient.setCache(cache)
            okHttpClient.networkInterceptors().add(Interceptor { chain ->
                val originalResponse = chain.proceed(chain.request());
                originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=180")
                        .build();
            })
        }
        return okHttpClient
    }

}
