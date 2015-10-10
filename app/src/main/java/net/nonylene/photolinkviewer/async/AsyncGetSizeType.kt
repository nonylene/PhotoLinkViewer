package net.nonylene.photolinkviewer.async

import android.graphics.BitmapFactory
import android.os.AsyncTask

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays

open class AsyncGetSizeType : AsyncTask<String, Int, AsyncGetSizeType.Result>() {
    private val png = byteArrayOf(137.toByte(), 80.toByte(), 78.toByte(), 71.toByte())
    private val gif = byteArrayOf(71.toByte(), 73.toByte(), 70.toByte(), 56.toByte())
    private val jpg = byteArrayOf(255.toByte(), 216.toByte(), 255.toByte())
    private val bmp = byteArrayOf(66.toByte(), 77.toByte())

    override fun doInBackground(vararg params: String): Result {
        val result = Result()

        try {
            // read binary and get type
            val yaBinary = ByteArray(4)

            val url = URL(params[0])
            val urlConnection = url.openConnection() as HttpURLConnection

            urlConnection.responseCode.let { code ->
                if (code < 200 || code >= 300) {
                    result.errorMessage =  "${code} - ${urlConnection.responseMessage}"
                    return result
                }
            }

            urlConnection.inputStream.let {
                it.read(yaBinary, 0, 4)
                it.close()
            }

            // get bitmap size
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(url.openStream(), null, options)

            result.type = getFileType(yaBinary)
            result.height = options.outHeight
            result.width = options.outWidth

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
