package net.nonylene.photolinkviewer.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.bumptech.glide.Glide
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.core.event.DownloadButtonEvent
import net.nonylene.photolinkviewer.core.tool.PLVUrl
import net.nonylene.photolinkviewer.core.tool.PLVUrlService
import net.nonylene.photolinkviewer.core.tool.createTwitterPLVUrls
import net.nonylene.photolinkviewer.core.view.TilePhotoView
import org.greenrobot.eventbus.EventBus
import twitter4j.Status
import java.text.SimpleDateFormat
import java.util.*

class UserTweetView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val baseView: LinearLayout by bindView(R.id.twBase)
    private val textView: TextView by bindView(R.id.twTxt)
    private val snView: TextView by bindView(R.id.twSN)
    private val dayView: TextView by bindView(R.id.twDay)
    private val likeView: TextView by bindView(R.id.likeCount)
    private val rtView: TextView by bindView(R.id.rtCount)
    private val iconView: ImageView by bindView(R.id.twImageView)
    private val urlBaseLayout: LinearLayout by bindView(R.id.url_base)
    private val urlLayout: LinearLayout by bindView(R.id.url_linear)
    private val urlPhotoLayout: TilePhotoView by bindView(R.id.url_photos)
    private val photoBaseLayout: LinearLayout by bindView(R.id.photo_base)
    private val photoLayout: TilePhotoView by bindView(R.id.photos)

    var tilePhotoViewListener: TilePhotoView.TilePhotoViewListener? = null
    var sendDownloadEvent = false

    var status : Status? = null
        private set

    private val DP = context.resources.displayMetrics.density

    fun setEntry(status: Status) {
        this.status = status

        //retweet check
        val finStatus = if (status.isRetweet) status.retweetedStatus else status

        // long tap to copy text
        baseView.setOnLongClickListener {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                    ClipData.newPlainText("tweet text", textView.text)
            Toast.makeText(context.applicationContext, "Tweet copied!", Toast.LENGTH_LONG).show()
            true
        }

        // put status on text
        textView.text = finStatus.text

        dayView.text = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(finStatus.createdAt)
        likeView.text = "Like: " + finStatus.favoriteCount
        rtView.text = "RT: " + finStatus.retweetCount

        finStatus.user.let { user ->
            snView.text = user.name + " @" + user.screenName

            if (user.isProtected) {
                // add key icon
                val iconSize = (17 * DP).toInt()
                // resize app icon (bitmap_factory makes low-quality images)
                val protect = ContextCompat.getDrawable(context, R.drawable.lock)
                protect.setBounds(0, 0, iconSize, iconSize)
                // set app-icon and bounds
                snView.setCompoundDrawables(protect, null, null, null)
            } else {
                // initialize
                snView.setCompoundDrawables(null, null, null, null)
            }
            // set icon
            Glide.with(context.applicationContext).load(user.biggerProfileImageURL).into(iconView)
            iconView.setBackgroundResource(R.drawable.twitter_image_design)
            //show user when tapped
            iconView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + user.screenName))
                context.startActivity(intent)
            }
        }

        finStatus.urlEntities.let { urlEntities ->
            // initialize
            urlLayout.removeAllViews()
            urlPhotoLayout.initialize()

            if (!urlEntities.isEmpty()) {
                urlBaseLayout.visibility = View.VISIBLE

                urlPhotoLayout.tilePhotoViewListener = tilePhotoViewListener

                for (urlEntity in urlEntities) {
                    val url = urlEntity.expandedURL
                    addUrl(url)
                    val service = PLVUrlService(context).apply {
                        plvUrlListener = getPLVUrlListener(urlPhotoLayout)
                    }
                    service.requestGetPLVUrl(url)
                }
            } else {
                urlBaseLayout.visibility = View.GONE
            }
        }

        finStatus.mediaEntities.let { mediaEntities ->
            // initialize
            photoLayout.initialize()
            if (!mediaEntities.isEmpty()) {
                photoBaseLayout.visibility = View.VISIBLE

                photoLayout.tilePhotoViewListener = tilePhotoViewListener

                val plvUrls = createTwitterPLVUrls(finStatus, context)

                if (sendDownloadEvent) EventBus.getDefault().postSticky(DownloadButtonEvent(plvUrls, true))

                photoLayout.setPLVUrls(photoLayout.addImageView(), plvUrls.toTypedArray())
                photoLayout.notifyChanged()
            } else {
                photoBaseLayout.visibility = View.GONE
            }
        }
    }


    private fun getPLVUrlListener(tileView: TilePhotoView): PLVUrlService.PLVUrlListener {
        return (object : PLVUrlService.PLVUrlListener {
            var position: Int? = null

            override fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
                tileView.setPLVUrls(position!!, plvUrls)
                tileView.notifyChanged()
            }

            override fun onGetPLVUrlFailed(text: String) {
                position?.let {
                    tileView.removeImageView(it)
                    tileView.notifyChanged()
                }
            }

            override fun onURLAccepted() {
                position = tileView.addImageView()
                tileView.notifyChanged()
            }
        })
    }

    private fun addUrl(url: String) {
        val textView = LayoutInflater.from(urlLayout.context).inflate(R.layout.twitter_url, urlLayout, false) as TextView
        textView.text = url
        textView.paint.isUnderlineText = true
        textView.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        urlLayout.addView(textView)
    }
}
