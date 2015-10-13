package net.nonylene.photolinkviewer.tool

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.android.volley.toolbox.ImageLoader

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.view.UserTweetLoadingView
import net.nonylene.photolinkviewer.view.UserTweetView

import java.util.ArrayList

import twitter4j.Status

class TwitterStatusAdapter(private val imageLoader: ImageLoader) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), UserTweetView.TwitterViewListener, UserTweetLoadingView.LoadingViewListener {

    private val statusList = ArrayList<Status?>()
    private var twitterAdapterListener: TwitterAdapterListener? = null

    private var isRequesting: Boolean = false
        set(value) {
            field = value
            notifyItemChanged(statusList.size() - 1)
        }

    val lastStatus: Status?
        get() {
            return statusList.last()?.let { it } ?: statusList.get(statusList.size() - 2)
        }

    override fun onShowFragmentRequired(plvUrl: PLVUrl) {
        twitterAdapterListener?.onShowFragmentRequired(plvUrl)
    }

    override fun onVideoShowFragmentRequired(plvUrl: PLVUrl) {
        twitterAdapterListener?.onVideoShowFragmentRequired(plvUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == ItemType.LOADING.id)
            return UserLoadingViewHolder(inflater.inflate(R.layout.loading_layout, parent, false))
        else
            return UserTweetViewHolder(inflater.inflate(R.layout.twitter_status_list, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewTypeEnum(position)) {
            ItemType.STATUS -> (holder as UserTweetViewHolder).setContent(statusList.get(position)!!)
            ItemType.LOADING -> (holder as UserLoadingViewHolder).setContent(isRequesting)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItemViewTypeEnum(position).id
    }

    private fun getItemViewTypeEnum(position: Int): ItemType {
        return statusList.get(position)?.let { ItemType.STATUS } ?: ItemType.LOADING
    }

    override fun getItemCount(): Int {
        return statusList.size()
    }

    fun setTwitterAdapterListener(twitterAdapterListener: TwitterAdapterListener) {
        this.twitterAdapterListener = twitterAdapterListener
    }

    public fun addItem(status: Status) {
        var startSize = statusList.size()

        // init -> add loading footer
        if (statusList.isEmpty()) statusList.addAll(arrayOf(status, null))
        else statusList.add(statusList.size() - 1, status)

        if (status.inReplyToScreenName == null) statusList.remove(statusList.size() - 1)

        notifyItemRangeInserted(startSize - 1, statusList.size() - startSize)

        if (status.inReplyToScreenName != null){
            // auto pager
            if (statusList.size() % 4 != 2) twitterAdapterListener?.onReadMoreClicked()
            else isRequesting = false
        }
    }

    override fun onReadMoreClicked() {
        twitterAdapterListener!!.onReadMoreClicked()
        isRequesting = true
    }

    private enum class ItemType internal constructor(val id: Int) {
        STATUS(0), LOADING(1)
    }

    private inner class UserTweetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tweetView : UserTweetView

        init {
            tweetView = itemView as UserTweetView
            tweetView.imageLoader = imageLoader
            tweetView.twitterViewListener = this@TwitterStatusAdapter
        }

        public fun setContent(status : Status) {
            tweetView.setEntry(status)
        }
    }

    private inner class UserLoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loadingView : UserTweetLoadingView

        init {
            loadingView = itemView as UserTweetLoadingView
            loadingView.loadingViewListener = this@TwitterStatusAdapter
        }

        public fun setContent(isRequesting : Boolean) {
            loadingView.setIsRequesting(isRequesting)
        }
    }

    interface TwitterAdapterListener {
        fun onShowFragmentRequired(plvUrl: PLVUrl)
        fun onVideoShowFragmentRequired(plvUrl: PLVUrl)
        fun onReadMoreClicked()
    }
}
