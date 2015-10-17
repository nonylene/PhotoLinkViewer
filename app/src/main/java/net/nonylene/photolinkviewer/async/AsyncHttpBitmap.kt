package net.nonylene.photolinkviewer.async

import android.content.AsyncTaskLoader
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.squareup.okhttp.Request
import net.nonylene.photolinkviewer.tool.OkHttpManager

import net.nonylene.photolinkviewer.tool.PLVUrl

import java.io.IOException

class AsyncHttpBitmap(context: Context, private val plvUrl: PLVUrl, private val max_size: Int) : AsyncTaskLoader<AsyncHttpBitmap.Result>(context) {

    private var result: Result? = null

    override fun loadInBackground(): Result {
        val httpResult = Result()

        try {
            val request = OkHttpManager.getOkHttpClient(context).newCall(
                    Request.Builder()
                            .url(plvUrl.displayUrl)
                            .get()
                            .build()
            ).execute()

            val inputStream = request.body().byteStream()

            // if bitmap size is bigger than limit, load small photo
            val bitmap: Bitmap
            if (max_size < Math.max(plvUrl.height, plvUrl.width)) {
                val options2 = BitmapFactory.Options().apply {
                    inSampleSize = Math.max(plvUrl.height, plvUrl.width) / max_size + 1
                }
                httpResult.apply {
                    isResized = true
                    originalWidth = plvUrl.height
                    originalHeight = plvUrl.width
                }
                bitmap = BitmapFactory.decodeStream(inputStream, null, options2)
            } else {
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
            inputStream.close()
            httpResult.bitmap = bitmap

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return httpResult
    }

    override fun deliverResult(httpResult: Result) {
        if (isReset) {
            this.result = null
            return
        }

        this.result = httpResult
        if (isStarted) super.deliverResult(httpResult)
    }

    public override fun onStartLoading() {
        this.result?.let { deliverResult(it) }
        if (takeContentChanged() || this.result == null) {
            forceLoad()
        }
    }

    public override fun onStopLoading() {
        super.onStopLoading()
        cancelLoad()
    }

    public override fun onReset() {
        super.onReset()
        onStopLoading()
    }

    inner class Result {
        var bitmap: Bitmap? = null
        var url: String? = null
        var originalWidth: Int = 0
        var originalHeight: Int = 0
        var isResized: Boolean = false
    }
}
