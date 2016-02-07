package net.nonylene.photolinkviewer.tool

import android.content.Context
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.OkHttpClient
import com.squareup.picasso.OkHttpDownloader
import com.squareup.picasso.Picasso
import net.nonylene.photolinkviewer.core.PhotoLinkViewer

object OkHttpManager {
    val okHttpClient by lazy {
        OkHttpClient().apply {
            PhotoLinkViewer.cache?.let { cache ->
                //enable cache
                setCache(cache)
                networkInterceptors().add(Interceptor { chain ->
                    val originalResponse = chain.proceed(chain.request());
                    originalResponse.newBuilder()
                            .header("Cache-Control", "public, max-age=180")
                            .build();
                })
            }
        }
    }
    private var picasso : Picasso? = null

    fun getPicasso(context: Context) : Picasso {
        if (picasso == null) {
            picasso = Picasso.Builder(context)
                    .downloader(OkHttpDownloader(okHttpClient))
                    .build()
        }
        return picasso!!
    }
}
