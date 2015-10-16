package net.nonylene.photolinkviewer.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.PLVUrlService
import twitter4j.Status
import java.text.SimpleDateFormat

class UserTweetView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var textView: TextView? = null
    private var snView: TextView? = null
    private var dayView: TextView? = null
    private var favView: TextView? = null
    private var rtView: TextView? = null
    private var iconView: NetworkImageView? = null
    private var urlBaseLayout: LinearLayout? = null
    private var urlLayout: LinearLayout? = null
    private var urlPhotoLayout: TilePhotoView? = null
    private var photoBaseLayout: LinearLayout? = null
    private var photoLayout: TilePhotoView? = null

    public var imageLoader: ImageLoader? = null
    public var twitterViewListener: TwitterViewListener? = null

    public var status : Status? = null
        private set

    private val DP = context.resources.displayMetrics.density

    protected override fun onFinishInflate() {
        super.onFinishInflate()
        textView = findViewById(R.id.twTxt) as TextView
        snView = findViewById(R.id.twSN) as TextView
        dayView = findViewById(R.id.twDay) as TextView
        favView = findViewById(R.id.favCount) as TextView
        rtView= findViewById(R.id.rtCount) as TextView
        iconView = findViewById(R.id.twImageView) as NetworkImageView
        urlBaseLayout = findViewById(R.id.url_base) as LinearLayout
        urlLayout = findViewById(R.id.url_linear) as LinearLayout
        urlPhotoLayout = findViewById(R.id.url_photos) as TilePhotoView
        photoBaseLayout = findViewById(R.id.photo_base) as LinearLayout
        photoLayout = findViewById(R.id.photos) as TilePhotoView
    }

    public fun setEntry(status: Status) {
        this.status = status

        //retweet check
        val finStatus = if (status.isRetweet) status.retweetedStatus else status

        // put status on text
        textView!!.text = finStatus.text
        dayView!!.text = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(finStatus.createdAt)
        favView!!.text = "fav: " + finStatus.favoriteCount
        rtView!!.text = "RT: " + finStatus.retweetCount

        finStatus.user.let { user ->
            snView!!.text = user.name + " @" + user.screenName

            if (user.isProtected) {
                // add key icon
                val iconSize = (17 * DP).toInt()
                // resize app icon (bitmap_factory makes low-quality images)
                val protect = context.resources.getDrawable(R.drawable.lock)
                protect.setBounds(0, 0, iconSize, iconSize)
                // set app-icon and bounds
                snView!!.setCompoundDrawables(protect, null, null, null)
            } else {
                // initialize
                snView!!.setCompoundDrawables(null, null, null, null)
            }
            // set icon
            iconView!!.setImageUrl(user.biggerProfileImageURL, imageLoader)
            iconView!!.setBackgroundResource(R.drawable.twitter_image_design)
            //show user when tapped
            iconView!!.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + user.screenName))
                context.startActivity(intent)
            }
        }

        status.urlEntities.let { urlEntities ->
            // initialize
            urlLayout!!.removeAllViews()
            urlPhotoLayout!!.removeAllViews()

            if (!urlEntities.isEmpty()) {
                urlBaseLayout!!.visibility = View.VISIBLE

                urlPhotoLayout!!.twitterViewListener = twitterViewListener
                urlPhotoLayout!!.imageLoader = imageLoader

                for (urlEntity in urlEntities) {
                    val url = urlEntity.expandedURL
                    addUrl(url)
                    val service = PLVUrlService(context, getPLVUrlListener(urlPhotoLayout!!))
                    service.requestGetPLVUrl(url)
                }
            } else {
                urlBaseLayout!!.visibility = View.GONE
            }
        }

        status.extendedMediaEntities.let { mediaEntities ->
            // initialize
            photoLayout!!.removeAllViews()
            if (!mediaEntities.isEmpty()) {
                photoBaseLayout!!.visibility = View.VISIBLE

                photoLayout!!.twitterViewListener = twitterViewListener
                photoLayout!!.imageLoader = imageLoader

                for (mediaEntity in mediaEntities) {
                    val url = mediaEntity.mediaURLHttps

                    if (mediaEntity.type in arrayOf("animated_gif", "video")) {
                        mediaEntity.videoAspectRatioHeight

                        // get biggest url
                        val file_url = mediaEntity.videoVariants.filter {
                            ("video/mp4") == it.contentType
                        }.maxBy { it.bitrate }!!.url

                        val plvUrl = PLVUrl(mediaEntity.mediaURLHttps)
                        plvUrl.siteName = "twitter"
                        plvUrl.thumbUrl = mediaEntity.mediaURLHttps
                        plvUrl.displayUrl = file_url
                        plvUrl.setIsVideo(true)
                        photoLayout!!.setVideoUrl(photoLayout!!.addImageView(), plvUrl)
                    } else {
                        val service = PLVUrlService(context, getPLVUrlListener(photoLayout!!))
                        service.requestGetPLVUrl(url)
                    }
                }
            } else {
                photoBaseLayout!!.visibility = View.GONE
            }
        }
    }


    private fun getPLVUrlListener(tileView: TilePhotoView): PLVUrlService.PLVUrlListener {
        return (object : PLVUrlService.PLVUrlListener {
            var position: Int? = null

            override public fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
                plvUrls.forEach { plvUrl ->
                    if (plvUrl.isVideo) tileView.setVideoUrl(position!!, plvUrl)
                    else tileView.setImageUrl(position!!, plvUrl)
                }
            }

            override public fun onGetPLVUrlFailed(text: String) {

            }

            override public fun onURLAccepted() {
                position = tileView.addImageView();
            }
        })
    }

    private fun addUrl(url: String) {
        val textView = LayoutInflater.from(urlLayout!!.context).inflate(R.layout.twitter_url, urlLayout, false) as TextView
        textView.text = url
        textView.paint.isUnderlineText = true
        textView.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        urlLayout!!.addView(textView)
    }

    public interface TwitterViewListener {
        fun onShowFragmentRequired(plvUrl: PLVUrl)
        fun onVideoShowFragmentRequired(plvUrl: PLVUrl)
    }
}
