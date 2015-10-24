package net.nonylene.photolinkviewer.async

import android.content.AsyncTaskLoader
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.squareup.okhttp.Request
import net.nonylene.photolinkviewer.tool.OkHttpManager

import net.nonylene.photolinkviewer.tool.PLVUrl
import java.io.BufferedInputStream

import java.io.IOException
import java.util.*

class AsyncHttpBitmap(context: Context, private val plvUrl: PLVUrl, private val max_size: Int) : AsyncTaskLoader<AsyncHttpBitmap.Result>(context) {

    private var result: Result? = null
    private val png = byteArrayOf(137.toByte(), 80.toByte(), 78.toByte(), 71.toByte())
    private val gif = byteArrayOf(71.toByte(), 73.toByte(), 70.toByte(), 56.toByte())
    private val jpg = byteArrayOf(255.toByte(), 216.toByte(), 255.toByte())
    private val bmp = byteArrayOf(66.toByte(), 77.toByte())

    override fun loadInBackground(): Result {
        val httpResult = Result()

        try {
            val response = OkHttpManager.getOkHttpClient(context).newCall(
                    Request.Builder()
                            .url(plvUrl.displayUrl)
                            .get()
                            .build()
            ).execute()

            if (!response.isSuccessful) {
                httpResult.errorMessage =  "${response.code()} - ${response.message()}"
                return httpResult
            }

            val inputStream = response.body().byteStream().let {
                if (it.markSupported()) it else BufferedInputStream(it)
            }

            inputStream.mark(65536);

            // get bitmap size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            httpResult.originalWidth = options.outWidth
            httpResult.originalHeight = options.outHeight
            inputStream.reset()

            // read binary and get type
            val yaBinary = ByteArray(4)
            inputStream.read(yaBinary, 0, 4)
            httpResult.type = getFileType(yaBinary)
            inputStream.reset()

            // if bitmap size is bigger than limit, load small photo
            val bitmap: Bitmap?
            if (max_size < Math.max(options.outHeight, options.outWidth)) {
                val options2 = BitmapFactory.Options().apply {
                    inSampleSize = Math.max(options.outHeight, options.outWidth) / max_size + 1
                }
                httpResult.isResized = true
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

    private fun getFileType(head: ByteArray): String {
        return when {
            Arrays.equals(Arrays.copyOfRange(head, 0, 4), png) -> "png"
            Arrays.equals(Arrays.copyOfRange(head, 0, 4), gif) -> "gif"
            Arrays.equals(Arrays.copyOfRange(head, 0, 3), jpg) -> "jpg"
            Arrays.equals(Arrays.copyOfRange(head, 0, 2), bmp) -> "bmp"
            else                                               -> "unknown"
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
        var type: String? = null
        var errorMessage: String? = null
    }
}
