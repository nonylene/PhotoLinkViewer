package net.nonylene.photolinkviewer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.tool.PLVUrl
import java.util.*

// TODO: use gridView to multiple photos
public class TilePhotoView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val imageViewList = ArrayList<NetworkImageView>()
    private val inflater : LayoutInflater

    public var twitterViewListener: UserTweetView.TwitterViewListener? = null
    public var imageLoader : ImageLoader? = null

    init {
        inflater = LayoutInflater.from(context)
        orientation = LinearLayout.VERTICAL
    }

    public fun addImageView(): Int {
        // prev is last linear_layout
        val size = imageViewList.size()

        val frameLayout: FrameLayout
        if (size % 2 == 0) {
            // make new linear_layout and put below prev
            val new_layout = inflater.inflate(R.layout.twitter_photos, this , false) as LinearLayout
            addView(new_layout)
            frameLayout = new_layout.getChildAt(0) as FrameLayout
        } else {
            val prevLayout = getChildAt(childCount - 1) as LinearLayout
            // put new photo below prev photo (not new linear_layout)
            frameLayout = prevLayout.getChildAt(1) as FrameLayout
        }
        imageViewList.add(frameLayout.getChildAt(0) as NetworkImageView)

        return size
    }

    public fun setImageUrl(position: Int, plvUrl: PLVUrl) {
        val imageView = imageViewList.get(position)

        imageView.setOnClickListener {
            twitterViewListener?.onShowFragmentRequired(plvUrl)
        }
        imageView.setImageUrl(plvUrl.thumbUrl, imageLoader)
    }

    public fun setVideoUrl(position: Int, plvUrl: PLVUrl) {
        val imageView = imageViewList.get(position)
        (imageView.parent as FrameLayout).getChildAt(1).visibility = View.VISIBLE

        imageView.setOnClickListener {
            twitterViewListener?.onVideoShowFragmentRequired(plvUrl)
        }
        imageView.setImageUrl(plvUrl.thumbUrl, imageLoader)
    }
}
