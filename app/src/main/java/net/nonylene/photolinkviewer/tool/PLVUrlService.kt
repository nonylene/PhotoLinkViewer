package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.util.Base64
import android.widget.Toast

import com.android.volley.Response
import com.android.volley.toolbox.Volley

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.async.AsyncGetURL

import org.json.JSONException
import org.json.JSONObject

import java.util.regex.Pattern

import javax.crypto.spec.SecretKeySpec

class PLVUrlService(private val context: Context, private val plvUrlListener: PLVUrlService.PLVUrlListener) {

    interface PLVUrlListener {
        fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>)
        fun onGetPLVUrlFailed(text: String)
        fun onURLAccepted()
    }

    fun requestGetPLVUrl(url: String) {
        val site = when {
            url.contains("flickr.com"), url.contains("flic.kr")
                                             -> FlickrSite(url, context, plvUrlListener)
            url.contains("nico.ms"), url.contains("seiga.nicovideo.jp")
                                             -> NicoSite(url, context, plvUrlListener)
            url.contains("twimg.com/media/") -> TwitterSite(url, context, plvUrlListener)
            url.contains("twipple.jp")       -> TwippleSite(url, context, plvUrlListener)
            url.contains("img.ly")           -> ImglySite(url, context, plvUrlListener)
            url.contains("instagram.com"), url.contains("instagr.am")
                                             -> InstagramSite(url, context, plvUrlListener)
            url.contains("gyazo.com")        -> GyazoSite(url, context, plvUrlListener)
            url.contains("imgur.com")        -> ImgurSite(url, context, plvUrlListener)
            url.contains("vine.co")          -> VineSite(url, context, plvUrlListener)
            else                             -> OtherSite(url, context, plvUrlListener)
        }
        site.getPLVUrl()
    }

    private abstract inner class Site(protected var url: String, protected var context: Context, protected val listener: PLVUrlListener) {

        protected fun wifiChecker(sharedPreferences: SharedPreferences): Boolean {
            //check wifi connecting and wifi setting enabled or not
            return sharedPreferences.getBoolean("wifi_switch", false) &&
                    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo.type ==
                            ConnectivityManager.TYPE_WIFI
        }

        protected fun getQuality(siteName: String): String {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("${siteName}_quality_" +
                    if (wifiChecker(sharedPreferences)) "wifi" else "3g", "large")
        }

        protected fun getId(url: String, regex: String): String? {
            val matcher = Pattern.compile(regex).matcher(url)
            if (!matcher.find()) {
                onParseFailed()
                return null
            }
            listener.onURLAccepted()
            return matcher.group(1)
        }

        protected fun onParseFailed() {
            listener.onGetPLVUrlFailed(context.getString(R.string.url_purse_toast))
        }

        abstract fun getPLVUrl()
    }

    private inner class TwitterSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://pbs\\.twimg\\.com/media/([^\\.]+)\\.")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "twitter"
                plvUrl.fileName = id

                plvUrl.biggestUrl = url + ":orig"
                plvUrl.thumbUrl = url + ":small"
                plvUrl.displayUrl = when (super.getQuality("twitter")) {
                    "original" -> plvUrl.biggestUrl
                    "large"    -> url + ":large"
                    "medium"   -> url
                    "small"    -> plvUrl.thumbUrl
                    else       -> null
                }

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class TwippleSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://p\\.twipple\\.jp/(\\w+)")?.let { id ->
                val plvUrl = PLVUrl(url)
                plvUrl.siteName = "twipple"
                plvUrl.fileName = id

                plvUrl.biggestUrl = "http://p.twipple.jp/show/orig/" + id
                plvUrl.thumbUrl = "http://p.twipple.jp/show/large/" + id
                plvUrl.displayUrl = when (super.getQuality("twipple")) {
                    "original" -> plvUrl.biggestUrl
                    "large"    -> plvUrl.thumbUrl
                    "thumb"    -> "http://p.twipple.jp/show/thumb/" + id
                    else       -> null
                }

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class ImglySite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://img\\.ly/(\\w+)")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "imgly"
                plvUrl.fileName = id

                plvUrl.biggestUrl = "http://img.ly/show/full/" + id
                plvUrl.thumbUrl = "http://img.ly/show/medium/" + id
                plvUrl.displayUrl = when (super.getQuality("imgly")) {
                    "full"   -> plvUrl.biggestUrl
                    "large"  -> plvUrl.thumbUrl
                    "medium" -> "http://img.ly/show/medium/" + id
                    else     -> null
                }

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class InstagramSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://instagr\\.?am[\\.com]*/p/([^/\\?=]+)")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "instagram"
                plvUrl.fileName = id

                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instagram_api", false)) {

                    val preferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE)

                    if (!preferences.getBoolean("instagram_authorized", false)) {
                        listener.onGetPLVUrlFailed("You have to authorize instagram account or change instagram api preference.")
                        return
                    }

                    val keyByte = Base64.decode(preferences.getString("instagram_key", null), Base64.DEFAULT)
                    val tokenByte = Base64.decode(preferences.getString("instagram_token", null), Base64.DEFAULT)
                    val token = Encryption.decrypt(tokenByte, SecretKeySpec(keyByte, 0, keyByte.size(), "AES"))
                    val apiUrl = "https://api.instagram.com/v1/media/shortcode/${id}?access_token=${token}"

                    Volley.newRequestQueue(context).add(MyJsonObjectRequest(context, apiUrl,
                            Response.Listener { response ->
                                try {
                                    listener.onGetPLVUrlFinished(arrayOf(parseInstagram(response, plvUrl)))
                                } catch (e: JSONException) {
                                    listener.onGetPLVUrlFailed("instagram JSON Parse Error!")
                                    e.printStackTrace()
                                }
                            })
                    );

                } else {
                    plvUrl.biggestUrl = "https://instagram.com/p/${id}/media/?size=l"
                    plvUrl.displayUrl = when (super.getQuality("instagram")) {
                        "large"  -> plvUrl.biggestUrl
                        "medium" -> "https://instagram.com/p/${id}/media/?size=m"
                        else     -> null
                    }
                    plvUrl.thumbUrl = "https://instagram.com/p/${id}/media/?size=m"

                    listener.onGetPLVUrlFinished(arrayOf(plvUrl))
                }

            }
        }

        @Throws(JSONException::class)
        private fun parseInstagram(json: JSONObject, plvUrl: PLVUrl): PLVUrl {
            //for flickr
            val data = JSONObject(json.getString("data"))
            val fileUrls: JSONObject

            if ("video" == data.getString("type")) {
                plvUrl.setIsVideo(true)
                fileUrls = data.getJSONObject("videos")
            } else {
                fileUrls = data.getJSONObject("images")
            }

            plvUrl.displayUrl =
                    when (super.getQuality("instagram")) {
                        "large"  -> fileUrls.getJSONObject("standard_resolution")
                        "medium" -> fileUrls.getJSONObject("low_resolution")
                        else     -> null
                    }!!.getString("url")

            val imageUrls = data.getJSONObject("images")
            plvUrl.thumbUrl = imageUrls.getJSONObject("low_resolution").getString("url")
            plvUrl.biggestUrl = imageUrls.getJSONObject("standard_resolution").getString("url")

            return plvUrl
        }
    }

    private inner class GyazoSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://.*gyazo\\.com/(\\w+)")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "gyazo"
                plvUrl.fileName = id

                plvUrl.displayUrl = "https://gyazo.com/${id}/raw"

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class ImgurSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://.*imgur\\.com/([\\w^\\.]+)")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "imgur"
                plvUrl.fileName = id

                val file_url = "http://i.imgur.com/${id}.jpg"
                plvUrl.displayUrl = file_url

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class OtherSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "/([^\\./]+)\\.(png|jpg|jpeg|gif)[\\w\\?=]*$")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "other"
                plvUrl.fileName = id

                plvUrl.displayUrl = url

                listener.onGetPLVUrlFinished(arrayOf(plvUrl))
            }
        }
    }

    private inner class FlickrSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            when {
                url.contains("flickr")  -> super.getId(url, "^https?://[wm]w*\\.flickr\\.com/?#?/photos/[\\w@]+/(\\d+)")
                url.contains("flic.kr") -> super.getId(url, "^https?://flic\\.kr/p/(\\w+)")?.let { Base58.decode(it) }
                else                    -> null
            }?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "flickr"
                plvUrl.fileName = id

                val api_key = context.getText(R.string.flickr_key) as String
                val request = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&format=json&nojsoncallback=1&api_key=${api_key}&photo_id=${id}"

                Volley.newRequestQueue(context).add(MyJsonObjectRequest(context, request,
                        Response.Listener { response ->
                            try {
                                listener.onGetPLVUrlFinished(arrayOf(parseFlickr(response, plvUrl)))
                            } catch (e: JSONException) {
                                listener.onGetPLVUrlFailed(context.getString(R.string.show_flickrjson_toast))
                                e.printStackTrace()
                            }
                        })
                );
            }
        }

        @Throws(JSONException::class)
        private fun parseFlickr(json: JSONObject, plvUrl: PLVUrl): PLVUrl {
            //for flickr
            val photo = JSONObject(json.getString("photo"))
            val farm = photo.getString("farm")
            val server = photo.getString("server")
            val id = photo.getString("id")
            val secret = photo.getString("secret")

            val original_secrets = photo.getString("originalsecret")
            val original_formats = photo.getString("originalformat")

            plvUrl.biggestUrl = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secrets + "_o." + original_formats
            plvUrl.thumbUrl = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_z.jpg"
            plvUrl.displayUrl = when (super.getQuality("flickr")) {
                "original" -> plvUrl.biggestUrl
                "large"    -> "https://farm${farm}.staticflickr.com/${server}/${id}_${secret}_b.jpg"
                "medium"   -> plvUrl.thumbUrl
                else       -> null
            }

            return plvUrl
        }
    }

    private inner class NicoSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            when {
                url.contains("nico.ms") -> super.getId(url, "^https?://nico\\.ms/im(\\d+)")
                else                    -> super.getId(url, "^https?://seiga.nicovideo.jp/seiga/im(\\d+)")
            }?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "nico"
                plvUrl.fileName = id

                val task = object : AsyncGetURL() {
                    override fun onPostExecute(redirect: String) {
                        super.onPostExecute(redirect)
                        listener.onGetPLVUrlFinished(arrayOf(parseNico(redirect, id, plvUrl)))
                    }
                }

                task.execute("http://seiga.nicovideo.jp/image/source/" + id)
            }
        }

        private fun parseNico(redirect: String, id: String, plvUrl: PLVUrl): PLVUrl {
            var biggest_url = redirect.replace("/o/", "/priv/")

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            val original: Boolean
            if (wifiChecker(sharedPreferences)) {
                original = sharedPreferences.getBoolean("original_switch_wifi", false)
            } else {
                original = sharedPreferences.getBoolean("original_switch_3g", false)
            }

            val quality = super.getQuality("nicoseiga")

            if (redirect.contains("account.nicovideo.jp") && (original || quality == "original")) {
                // cannot preview original photo
                biggest_url = "http://lohas.nicoseiga.jp/img/" + id + "l"
                Toast.makeText(context, context.getString(R.string.nico_original_toast), Toast.LENGTH_LONG).show()
            }

            plvUrl.biggestUrl = biggest_url
            plvUrl.thumbUrl = "http://lohas.nicoseiga.jp/img/" + id + "m"
            plvUrl.displayUrl = when (quality) {
                "original" -> biggest_url
                "large"    -> "http://lohas.nicoseiga.jp/img/" + id + "l"
                "medium"   -> plvUrl.thumbUrl
                else       -> null
            }

            return plvUrl
        }
    }

    private inner class VineSite(url: String, context: Context, listener: PLVUrlListener) : Site(url, context, listener) {

        override fun getPLVUrl() {
            super.getId(url, "^https?://vine\\.co/v/(\\w+)")?.let { id ->
                val plvUrl = PLVUrl(url)

                plvUrl.siteName = "vine"
                plvUrl.setIsVideo(true)
                plvUrl.fileName = id

                val request = "https://api.vineapp.com/timelines/posts/s/" + id

                Volley.newRequestQueue(context).add(MyJsonObjectRequest(context, request,
                        Response.Listener { response ->
                            try {
                                listener.onGetPLVUrlFinished(arrayOf(parseVine(response, plvUrl)))
                            } catch (e: JSONException) {
                                listener.onGetPLVUrlFailed(context.getString(R.string.show_flickrjson_toast))
                                e.printStackTrace()
                            }
                        })
                );
            }
        }

        @Throws(JSONException::class)
        private fun parseVine(json: JSONObject, plvUrl: PLVUrl): PLVUrl {
            val records = json.getJSONObject("data").getJSONArray("records").getJSONObject(0)
            plvUrl.displayUrl = records.getString("videoUrl")
            plvUrl.thumbUrl = records.getString("thumbnailUrl")
            return plvUrl
        }
    }
}
