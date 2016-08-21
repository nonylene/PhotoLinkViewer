package net.nonylene.photolinkviewer

import android.animation.LayoutTransition
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import butterknife.bindView
import net.nonylene.photolinkviewer.core.fragment.*
import net.nonylene.photolinkviewer.core.tool.PLVUrl
import net.nonylene.photolinkviewer.core.tool.PLVUrlService
import net.nonylene.photolinkviewer.core.tool.ProgressBarListener
import net.nonylene.photolinkviewer.core.view.TilePhotoView
import net.nonylene.photolinkviewer.tool.*

import net.nonylene.photolinkviewer.view.HeightScalableScrollView
import net.nonylene.photolinkviewer.view.UserTweetLoadingView
import net.nonylene.photolinkviewer.view.UserTweetView

import java.util.regex.Pattern

import twitter4j.AsyncTwitter
import twitter4j.Status
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterMethod


class TwitterDisplay : AppCompatActivity(), TwitterStatusAdapter.TwitterAdapterListener, ProgressBarListener, TilePhotoView.TilePhotoViewListener, UserTweetLoadingView.LoadingViewListener {

    private var url: String? = null
    private var statusAdapter: TwitterStatusAdapter? = null
    private var twitter: AsyncTwitter? = null
    private var isSingle: Boolean = false
    private var isInitialized = false

    private val mTwitterSingleScrollView : HeightScalableScrollView by bindView(R.id.twitter_single_scroll)
    private val mTwitterSingleView: UserTweetView by bindView(R.id.twitter_single_view)
    private val mTwitterSingleLoadingView: UserTweetLoadingView by bindView(R.id.twitter_single_loading)
    private val mTwitterSingleDivider: View by bindView(R.id.twitter_single_divider)
    private val mProgressBar: ProgressBar by bindView(R.id.show_progress)
    private val mTweetBaseLayout: LinearLayout by bindView(R.id.tweet_base_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_twitter_display)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            @Suppress
            mTweetBaseLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }

        if (Intent.ACTION_VIEW != intent.action) {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show()
            return
        }

        val bundle = Bundle()
        url = intent.data.toString()
        bundle.putString("url", url)

        if (!getSharedPreferences("preference", Context.MODE_PRIVATE).isTwitterOAuthed()) {
            Toast.makeText(applicationContext, getString(R.string.twitter_display_oauth), Toast.LENGTH_LONG).show()
            startActivity(Intent(this, TwitterOAuthActivity::class.java))
            finish()
            return
        }

        mTwitterSingleView.tilePhotoViewListener = this
        mTwitterSingleView.visibility = View.GONE

        mTwitterSingleLoadingView.visibility = View.GONE
        mTwitterSingleLoadingView.loadingViewListener = this

        mTwitterSingleScrollView.isVerticalScrollBarEnabled = false

        val matcher = Pattern.compile("^https?://(mobile\\.|)twitter\\.com/[^\\/]+/status(es|)/(\\d+)").matcher(url)
        if (!matcher.find()) return
        val id_long = java.lang.Long.parseLong(matcher.group(3))

        try {
            twitter = MyAsyncTwitter.getAsyncTwitter(applicationContext).apply {
                addListener(twitterListener)
                showStatus(id_long)
            }
        } catch (e: SQLiteException) {
            e.printStackTrace()

        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show()
            val intent = Intent(this, TwitterOAuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        // option fragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val optionFragment = OptionFragment().apply {
            arguments = OptionFragment.createArguments(url!!, id_long, getSharedPreferences("preference", MODE_PRIVATE).getDefaultTwitterScreenName()!!)
        }
        fragmentTransaction.add(R.id.root_layout, optionFragment).commit()
    }

    override fun onShowFragmentRequired(plvUrl: PLVUrl) {
        onFragmentRequired(ShowFragment(), ShowFragment.createArguments(plvUrl, isSingle), BaseShowFragment.SHOW_FRAGMENT_TAG)
    }

    override fun onVideoShowFragmentRequired(plvUrl: PLVUrl) {
        onFragmentRequired(VideoShowFragment(), VideoShowFragment.createArguments(plvUrl, isSingle), BaseShowFragment.SHOW_FRAGMENT_TAG)
    }

    private fun onFragmentRequired(fragment: Fragment, bundle: Bundle, tag: String) {
        try {
            // go to show fragment
            val fragmentTransaction = supportFragmentManager.beginTransaction()

            fragment.arguments = bundle
            fragmentTransaction.replace(R.id.show_frag_replace, fragment, tag)

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
            replyId = mTwitterSingleView.status!!.inReplyToStatusId
            mTwitterSingleScrollView.max = (resources.displayMetrics.heightPixels / 3.5f).toInt()
            mTwitterSingleScrollView.isVerticalScrollBarEnabled = true
            mTweetBaseLayout.setGravity(Gravity.CENTER_HORIZONTAL)
        }
        twitter!!.showStatus(replyId)
    }

    override fun hideProgressBar() {
        mProgressBar.visibility = View.GONE
    }

    class ChangeAccountDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val accountsList = MyAsyncTwitter.getAccountsList(activity)
            val screen_list = accountsList.screenList
            val row_id_list = accountsList.rowIdList
            // get current screen_name
            val sharedPreferences = activity.getSharedPreferences("preference", Context.MODE_PRIVATE)
            val current_name = sharedPreferences.getDefaultTwitterScreenName()
            return AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.account_dialog_title))
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setSingleChoiceItems(screen_list.toTypedArray(), screen_list.indexOf(current_name),
                            { dialogInterface, position ->
                                // save rowid and screen name to preference
                                sharedPreferences.edit()
                                        .putInt("account", row_id_list[position])
                                        .putDefaultTwitterScreenName(screen_list[position])
                                        .apply()
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
            // 179: not authorized
            // 88: limit exceeded
            // 64: suspended
            // 136: blocked
            when (e.errorCode) {
                179, 88, 64, 136 -> createAccountDialog()
            }
            runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
        }

        fun createAccountDialog() {
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
                            && (url!!.contains("/photo") || url!!.contains("/video")) && mediaEntities.size == 1) {
                        isSingle = true
                        val mediaEntity = mediaEntities[0]

                        if (mediaEntity.type in arrayOf("animated_gif", "video")) {
                            val displayUrl = mediaEntity.videoVariants.filter {
                                ("video/mp4") == it.contentType
                            }.maxBy { it.bitrate }!!.url
                            val fileName = Uri.parse(displayUrl).lastPathSegment?.let {
                                it.substring(0, it.lastIndexOf("."))
                            }
                            val plvUrl = PLVUrl(url!!, "twitter", fileName!!)
                            plvUrl.type = "mp4"
                            plvUrl.displayUrl = displayUrl
                            plvUrl.thumbUrl = mediaEntity.mediaURLHttps
                            plvUrl.isVideo = true
                            // get biggest url
                            onVideoShowFragmentRequired(plvUrl)

                        } else {
                            PLVUrlService(this@TwitterDisplay).apply {
                                plvUrlListener =  object : PLVUrlService.PLVUrlListener {
                                    override fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
                                        val plvUrl = plvUrls[0]
                                        if (plvUrl.isVideo) onVideoShowFragmentRequired(plvUrl)
                                        else onShowFragmentRequired(plvUrl)
                                    }

                                    override fun onGetPLVUrlFailed(text: String) {}
                                    override fun onURLAccepted() {}
                                }
                            }.requestGetPLVUrl(mediaEntity.mediaURLHttps)
                        }

                    } else {
                        hideProgressBar()
                        mTwitterSingleView.visibility = View.VISIBLE
                        mTwitterSingleView.sendDownloadEvent = true
                        mTwitterSingleView.setEntry(status)
                        if (status.inReplyToStatusId != (-1).toLong()) {
                            mTwitterSingleLoadingView.visibility = View.VISIBLE
                            mTwitterSingleDivider.visibility = View.VISIBLE
                        }

                    }
                } else {
                    if (statusAdapter == null) {
                        statusAdapter = TwitterStatusAdapter().apply {
                            twitterAdapterListener = this@TwitterDisplay
                        }
                        val listView = LayoutInflater.from(this@TwitterDisplay).inflate(R.layout.tweets_list_view, mTweetBaseLayout, false) as ListView
                        listView.adapter = statusAdapter
                        mTweetBaseLayout.addView(listView)
                        mTwitterSingleLoadingView.visibility = View.GONE
                        mTwitterSingleDivider.visibility = View.GONE
                    }
                    statusAdapter!!.addItem(status)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        twitter?.shutdown()
    }
}
