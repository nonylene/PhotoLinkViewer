package net.nonylene.photolinkviewer.async

import android.content.Context
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.squareup.okhttp.Request
import net.nonylene.photolinkviewer.tool.OkHttpManager

import java.io.IOException
import java.util.Arrays

open class AsyncGetSizeType(private val context: Context) : AsyncTask<String, Int, AsyncGetSizeType.Result>() {
    private val png = byteArrayOf(137.toByte(), 80.toByte(), 78.toByte(), 71.toByte())
    private val gif = byteArrayOf(71.toByte(), 73.toByte(), 70.toByte(), 56.toByte())
    private val jpg = byteArrayOf(255.toByte(), 216.toByte(), 255.toByte())
    private val bmp = byteArrayOf(66.toByte(), 77.toByte())

    override fun doInBackground(vararg params: String): Result {
        val result = Result()

        val client = OkHttpManager.getOkHttpClient(context)

        val request = Request.Builder()
                            .url(params[0])
                            .get()
                            .build()

        try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    result.errorMessage =  "${response.code()} - ${response.message()}"
                    return result
                }

                // read binary and get type
                val yaBinary = ByteArray(4)

                response.body().byteStream().let {
                    it.read(yaBinary, 0, 4)
                    it.close()
                }

                result.type = getFileType(yaBinary)
            }

            // get bitmap size
            BitmapFactory.Options().let { options ->
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(client.newCall(request).execute().body().byteStream(), null, options)

                result.height = options.outHeight
                result.width = options.outWidth
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result
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

    inner class Result {
        var height: Int? = null
        var width: Int? = null
        var type: String? = null
        var errorMessage: String? = null
    }
}
