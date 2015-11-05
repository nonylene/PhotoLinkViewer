package net.nonylene.photolinkviewer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.squareup.picasso.Picasso
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.tool.OkHttpManager
import net.nonylene.photolinkviewer.tool.PLVUrl
import java.util.*

public class TilePhotoView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    // null -> canceled
    // arrayListOf(null) -> empty view
    private val frameLayoutListList = ArrayList<ArrayList<PLVUrl?>?>()
    private val inflater : LayoutInflater

    public var twitterViewListener: UserTweetView.TwitterViewListener? = null
    private val picasso : Picasso

    init {
        inflater = LayoutInflater.from(context)
        orientation = LinearLayout.VERTICAL
        picasso = OkHttpManager.getPicasso(context)
    }

    // add empty view
    public fun addImageView(): Int {
        // prev is last linear_layout
        frameLayoutListList.add(arrayListOf(null))
        return frameLayoutListList.size - 1
    }

    // remove canceled view
    public fun removeImageView(position: Int) {
        frameLayoutListList.set(position, null)
    }

    public fun setPLVUrl(position: Int, plvUrl: PLVUrl) {
        if (frameLayoutListList.size > position) frameLayoutListList.set(position, arrayListOf(plvUrl))
    }

    public fun setPLVUrls(position: Int, plvUrls: Array<PLVUrl>) {
        if (frameLayoutListList.size > position) frameLayoutListList.set(position, plvUrls.toArrayList())
    }

    public fun notifyChanged() {
        val frameLayoutCombinedList = frameLayoutListList.fold(ArrayList<PLVUrl?>()) { combined, list ->
            list?.let { combined.addAll(it) }
            combined
        }

        frameLayoutCombinedList.withIndex().forEach { iv ->
            val frameLayout: FrameLayout

            if (childCount > iv.index / 2) {
                frameLayout = (getChildAt((iv.index / 2).toInt()) as LinearLayout).getChildAt(iv.index % 2) as FrameLayout
            } else {
                // new generation
                if (iv.index % 2 == 0) {
                    // make new linear_layout and put below prev
                    val new_layout = inflater.inflate(R.layout.twitter_photos, this , false) as LinearLayout
                    addView(new_layout)
                    frameLayout = new_layout.getChildAt(0) as FrameLayout
                } else {
                    val prevLayout = getChildAt(childCount - 1) as LinearLayout
                    // put new photo below prev photo (not new linear_layout)
                    frameLayout = prevLayout.getChildAt(1) as FrameLayout
                }
            }

            val imageView = frameLayout.getChildAt(0) as ImageView

            iv.value?.let { plv ->
                if (plv.isVideo) {
                    frameLayout.getChildAt(1).visibility = View.VISIBLE
                    imageView.setOnClickListener {
                        twitterViewListener?.onVideoShowFragmentRequired(plv)
                    }
                } else {
                    imageView.setOnClickListener {
                        twitterViewListener?.onShowFragmentRequired(plv)
                    }
                }
                picasso.load(plv.thumbUrl).into(imageView)
            }
        }

        // reverse -> not removed
        ((frameLayoutCombinedList.size + 1) / 2..childCount - 1).forEach { removeViewAt(it) }
    }

    public fun initialize() {
        removeAllViews()
        frameLayoutListList.clear()
    }
}
