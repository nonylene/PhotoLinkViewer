package net.nonylene.photolinkviewer

import android.animation.LayoutTransition
import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.http.HttpResponseCache
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*

import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley

import net.nonylene.photolinkviewer.fragment.OptionFragment
import net.nonylene.photolinkviewer.fragment.ShowFragment
import net.nonylene.photolinkviewer.fragment.TwitterOptionFragment
import net.nonylene.photolinkviewer.fragment.VideoShowFragment
import net.nonylene.photolinkviewer.tool.BitmapCache
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.PLVUrlService
import net.nonylene.photolinkviewer.tool.ProgressBarListener
import net.nonylene.photolinkviewer.tool.TwitterStatusAdapter
import net.nonylene.photolinkviewer.view.HeightScalableScrollView
import net.nonylene.photolinkviewer.view.UserTweetLoadingView
import net.nonylene.photolinkviewer.view.UserTweetView

import java.io.File
import java.io.IOException
import java.util.regex.Pattern

import twitter4j.AsyncTwitter
import twitter4j.Status
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterMethod


class TwitterDisplay : Activity(), TwitterStatusAdapter.TwitterAdapterListener, ProgressBarListener, UserTweetView.TwitterViewListener, UserTweetLoadingView.LoadingViewListener {

    private var url: String? = null
    private var statusAdapter: TwitterStatusAdapter? = null
    private var twitter: AsyncTwitter? = null
    private var isSingle: Boolean = false
    private var isInitialized = false
    private var mImageLoader: ImageLoader? = null

    private var mTwitterSingleScrollView : HeightScalableScrollView? = null
    private var mTwitterSingleView: UserTweetView? = null
    private var mTwitterSingleLoadingView: UserTweetLoadingView? = null
    private var mTwitterSingleDivider: View? = null
    private var mProgressBar: ProgressBar? = null
    private var mTweetBaseLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_twitter_display)

        mTwitterSingleScrollView = findViewById(R.id.twitter_single_scroll) as HeightScalableScrollView
        mTwitterSingleView = findViewById(R.id.twitter_single_view) as UserTweetView
        mTwitterSingleLoadingView = findViewById(R.id.twitter_single_loading) as UserTweetLoadingView
        mTwitterSingleDivider = findViewById(R.id.twitter_single_divider) as ImageView
        mProgressBar = findViewById(R.id.show_progress) as ProgressBar
        mTweetBaseLayout = findViewById(R.id.tweet_base_view) as LinearLayout

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mTweetBaseLayout!!.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }

        //enable cache
        try {
            val httpCacheDir = File(applicationContext.cacheDir, "http")
            val httpCacheSize = 10 * 1024 * 1024.toLong() // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.d("cache", "HTTP response cache installation failed")
        }

        if (Intent.ACTION_VIEW != intent.action) {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show()
            return
        }

        val bundle = Bundle()
        url = intent.data.toString()
        bundle.putString("url", url)

        // option fragment
        val fragmentTransaction = fragmentManager.beginTransaction()
        val optionFragment = OptionFragment()
        optionFragment.arguments = bundle
        fragmentTransaction.add(R.id.root_layout, optionFragment).commit()

        if (!getSharedPreferences("preference", Context.MODE_PRIVATE).getBoolean("authorized", false)) {
            Toast.makeText(applicationContext, getString(R.string.twitter_display_oauth), Toast.LENGTH_LONG).show()
            val intent = Intent(this, TOAuth::class.java)
            startActivity(intent)
            finish()
            return
        }

        mImageLoader = ImageLoader(Volley.newRequestQueue(applicationContext), BitmapCache())
        mTwitterSingleView!!.imageLoader = mImageLoader
        mTwitterSingleView!!.twitterViewListener = this
        mTwitterSingleView!!.visibility = View.GONE

        mTwitterSingleLoadingView!!.visibility = View.GONE
        mTwitterSingleLoadingView!!.loadingViewListener = this

        mTwitterSingleScrollView!!.isVerticalScrollBarEnabled = false

        val matcher = Pattern.compile("^https?://twitter\\.com/\\w+/status[es]*/(\\d+)").matcher(url)
        if (!matcher.find()) return
        val id_long = java.lang.Long.parseLong(matcher.group(1))

        try {
            twitter = MyAsyncTwitter.getAsyncTwitter(applicationContext)
            twitter!!.addListener(twitterListener)
            twitter!!.showStatus(id_long)

            bundle.putLong("id_long", id_long)
            val twitterOptionFragment = TwitterOptionFragment()
            twitterOptionFragment.arguments = bundle
            fragmentManager.beginTransaction().add(R.id.buttons, twitterOptionFragment).commit()

        } catch (e: SQLiteException) {
            e.printStackTrace()

        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show()
            val intent = Intent(this, TOAuth::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onShowFragmentRequired(plvUrl: PLVUrl) {
        onFragmentRequired(ShowFragment(), plvUrl)
    }

    override fun onVideoShowFragmentRequired(plvUrl: PLVUrl) {
        onFragmentRequired(VideoShowFragment(), plvUrl)
    }

    private fun onFragmentRequired(fragment: Fragment, plvUrl: PLVUrl) {
        try {
            // go to show fragment
            val bundle = Bundle()
            bundle.putParcelable("plvurl", plvUrl)
            bundle.putBoolean("single_frag", isSingle)
            val fragmentTransaction = fragmentManager.beginTransaction()

            fragment.arguments = bundle
            fragmentTransaction.replace(R.id.show_frag_replace, fragment)

            // back to this screen when back pressed
            if (!isSingle) fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

    }

    override fun onReadMoreClicked() {
        val replyId: Long

        if (statusAdapter != null) {
            replyId = statusAdapter!!.lastStatus!!.inReplyToStatusId
        } else {
            replyId = mTwitterSingleView!!.status!!.inReplyToStatusId
            mTwitterSingleScrollView!!.max = (resources.displayMetrics.heightPixels / 3.5f).toInt()
            mTwitterSingleScrollView!!.isVerticalScrollBarEnabled = true
            mTweetBaseLayout!!.setGravity(Gravity.CENTER_HORIZONTAL)
        }
        twitter!!.showStatus(replyId)
    }

    override fun hideProgressBar() {
        mProgressBar!!.visibility = View.GONE
    }

    class ChangeAccountDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val accountsList = MyAsyncTwitter.getAccountsList(activity)
            val screen_list = accountsList.screenList
            val row_id_list = accountsList.rowIdList
            // get current screen_name
            val sharedPreferences = activity.getSharedPreferences("preference", Context.MODE_PRIVATE)
            val current_name = sharedPreferences.getString("screen_name", null)
            return AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.account_dialog_title))
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setSingleChoiceItems(screen_list.toTypedArray(), screen_list.indexOf(current_name),
                            DialogInterface.OnClickListener { dialogInterface, position ->
                                // save rowid and screen name to preference
                                sharedPreferences.edit().putInt("account", row_id_list.get(position)).apply()
                                sharedPreferences.edit().putString("screen_name", screen_list.get(position)).apply()
                                //reload activity
                                startActivity(activity.intent)
                                dialog.dismiss()
                                activity.finish()
                            })
                    .create()
        }
    }

    private val twitterListener = object : TwitterAdapter() {

        override fun onException(e: TwitterException, twitterMethod: TwitterMethod?) {
            Log.e("twitterException", e.toString())
            val message = getString(R.string.twitter_error_toast) + ": " + e.statusCode + "\n(" + e.errorMessage + ")"
            when (e.errorCode) {
                179, 88 -> createDialog()
            }
            runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
        }

        fun createDialog() {
            val accountDialog = ChangeAccountDialog()
            accountDialog.show(fragmentManager, "account")
        }

        override fun gotShowStatus(status: Status) {
            runOnUiThread{
                if (!isInitialized) {
                    isInitialized = true

                    // set media entity
                    val mediaEntities = status.extendedMediaEntities

                    // if number of media entity is one, show fragment directly
                    if (!PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("disp_tweet", false)
                            && (url!!.contains("/photo") || url!!.contains("/video")) && mediaEntities.size() == 1) {
                        isSingle = true
                        val mediaEntity = mediaEntities[0]

                        if (mediaEntity.type in arrayOf("animated_gif", "video")) {
                            val plvUrl = PLVUrl(url)
                            // get biggest url
                            plvUrl.displayUrl = mediaEntity.videoVariants.filter {
                                ("video/mp4") == it.contentType
                            }.maxBy { it.bitrate }!!.url
                            onVideoShowFragmentRequired(plvUrl)

                        } else {
                            PLVUrlService(this@TwitterDisplay, object : PLVUrlService.PLVUrlListener {
                                override fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
                                    val plvUrl = plvUrls[0]
                                    if (plvUrl.isVideo) onVideoShowFragmentRequired(plvUrl)
                                    else onShowFragmentRequired(plvUrl)
                                }

                                override fun onGetPLVUrlFailed(text: String) {}
                                override fun onURLAccepted() {}
                            }).requestGetPLVUrl(mediaEntity.mediaURLHttps)
                        }

                    } else {
                        hideProgressBar()
                        mTwitterSingleView!!.visibility = View.VISIBLE
                        mTwitterSingleView!!.setEntry(status)
                        if (status.inReplyToScreenName != null) {
                            mTwitterSingleLoadingView!!.visibility = View.VISIBLE
                            mTwitterSingleDivider!!.visibility = View.VISIBLE
                        }

                    }
                } else {
                    if (statusAdapter == null) {
                        statusAdapter = TwitterStatusAdapter(mImageLoader!!)
                        statusAdapter!!.twitterAdapterListener = this@TwitterDisplay
                        val listView = LayoutInflater.from(this@TwitterDisplay).inflate(R.layout.tweets_list_view, mTweetBaseLayout, false) as ListView
                        listView.adapter = statusAdapter
                        mTweetBaseLayout!!.addView(listView)
                        mTwitterSingleLoadingView!!.visibility = View.GONE
                        mTwitterSingleDivider!!.visibility = View.GONE
                    }
                    statusAdapter!!.addItem(status)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        HttpResponseCache.getInstalled()?.flush()
    }
}
