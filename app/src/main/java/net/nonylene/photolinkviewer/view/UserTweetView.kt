package net.nonylene.photolinkviewer.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.PLVUrlService
import twitter4j.ExtendedMediaEntity
import twitter4j.Status
import java.text.SimpleDateFormat
import java.util.*

class UserTweetView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var textView: TextView? = null
    private var snView: TextView? = null
    private var dayView: TextView? = null
    private var favView: TextView? = null
    private var rtView: TextView? = null
    private var iconView: NetworkImageView? = null
    private var urlBaseLayout: LinearLayout? = null
    private var urlLayout: LinearLayout? = null
    private var urlPhotoLayout: LinearLayout? = null
    private var photoBaseLayout: LinearLayout? = null
    private var photoLayout: LinearLayout? = null

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
        urlPhotoLayout = findViewById(R.id.url_photos) as LinearLayout
        photoBaseLayout = findViewById(R.id.photo_base) as LinearLayout
        photoLayout = findViewById(R.id.photos) as LinearLayout
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

                val controller = PhotoViewController(urlPhotoLayout!!)

                for (urlEntity in urlEntities) {
                    val url = urlEntity.expandedURL
                    addUrl(url)
                    val service = PLVUrlService(context, getPLVUrlListener(controller))
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

                val controller = PhotoViewController(photoLayout!!)

                for (mediaEntity in mediaEntities) {
                    val url = mediaEntity.mediaURLHttps

                    if (mediaEntity.type in arrayOf("animated_gif", "video")) {
                        mediaEntity.videoAspectRatioHeight
                        val file_url = getBiggestMp4Url(mediaEntity.videoVariants)
                        val plvUrl = PLVUrl(mediaEntity.mediaURLHttps)
                        plvUrl.siteName = "twitter"
                        plvUrl.thumbUrl = mediaEntity.mediaURLHttps
                        plvUrl.displayUrl = file_url
                        plvUrl.setIsVideo(true)
                        controller.setVideoUrl(controller.addImageView(), plvUrl)
                    } else {
                        val service = PLVUrlService(context, getPLVUrlListener(controller))
                        service.requestGetPLVUrl(url)
                    }
                }
            } else {
                photoBaseLayout!!.visibility = View.GONE
            }
        }
    }


    private fun getPLVUrlListener(controller: PhotoViewController): PLVUrlService.PLVUrlListener {
        return (object : PLVUrlService.PLVUrlListener {
            var position: Int? = null

            override public fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
                val plvUrl = plvUrls[0]

                if (plvUrl.isVideo) controller.setVideoUrl(position!!, plvUrl)
                else controller.setImageUrl(position!!, plvUrl)
            }

            override public fun onGetPLVUrlFailed(text: String) {

            }

            override public fun onURLAccepted() {
                position = controller.addImageView();
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

    private fun getBiggestMp4Url(variants: Array<ExtendedMediaEntity.Variant>): String {
        return variants.filter {
            ("video/mp4") == it.contentType
        }.maxBy { it.bitrate }!!.url
    }

    private inner class PhotoViewController(private val baseLayout: LinearLayout) {
        private val imageViewList = ArrayList<NetworkImageView>()
        private val inflater: LayoutInflater

        init {
            inflater = LayoutInflater.from(baseLayout.context)
        }

        fun addImageView(): Int {
            // prev is last linear_layout
            val size = imageViewList.size()

            val frameLayout: FrameLayout
            if (size % 2 == 0) {
                // make new linear_layout and put below prev
                val new_layout = inflater.inflate(R.layout.twitter_photos, baseLayout, false) as LinearLayout
                baseLayout.addView(new_layout)
                frameLayout = new_layout.getChildAt(0) as FrameLayout
            } else {
                val prevLayout = baseLayout.getChildAt(baseLayout.childCount - 1) as LinearLayout
                // put new photo below prev photo (not new linear_layout)
                frameLayout = prevLayout.getChildAt(1) as FrameLayout
            }
            imageViewList.add(frameLayout.getChildAt(0) as NetworkImageView)

            return size
        }

        fun setImageUrl(position: Int, plvUrl: PLVUrl) {
            val imageView = imageViewList.get(position)

            imageView.setOnClickListener {
                twitterViewListener?.onShowFragmentRequired(plvUrl)
            }
            imageView.setImageUrl(plvUrl.thumbUrl, imageLoader)
        }

        fun setVideoUrl(position: Int, plvUrl: PLVUrl) {
            val imageView = imageViewList.get(position)
            (imageView.parent as FrameLayout).getChildAt(1).visibility = View.VISIBLE

            imageView.setOnClickListener {
                twitterViewListener?.onVideoShowFragmentRequired(plvUrl)
            }
            imageView.setImageUrl(plvUrl.thumbUrl, imageLoader)
        }
    }

    public interface TwitterViewListener {
        fun onShowFragmentRequired(plvUrl: PLVUrl)
        fun onVideoShowFragmentRequired(plvUrl: PLVUrl)
    }
}
