package net.nonylene.photolinkviewer

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley

import net.nonylene.photolinkviewer.fragment.OptionFragment
import net.nonylene.photolinkviewer.fragment.ShowFragment
import net.nonylene.photolinkviewer.fragment.VideoShowFragment
import net.nonylene.photolinkviewer.tool.BitmapCache
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.PLVUrlService
import net.nonylene.photolinkviewer.tool.ProgressBarListener
import net.nonylene.photolinkviewer.view.TilePhotoView
import net.nonylene.photolinkviewer.view.UserTweetView

import java.io.File
import java.io.IOException

class Show : Activity(), PLVUrlService.PLVUrlListener, ProgressBarListener, UserTweetView.TwitterViewListener {

    private var isSingle : Boolean = true
    private var scrollView : ScrollView? = null
    private var tileView : TilePhotoView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)

        scrollView = findViewById(R.id.show_activity_scroll) as ScrollView
        tileView = findViewById(R.id.show_activity_tile) as TilePhotoView

        //enable cache
        try {
            val httpCacheDir = File(applicationContext.cacheDir, "http")
            val httpCacheSize = 10 * 1024 * 1024.toLong() // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.d("cache", "HTTP response cache installation failed")
        }

        //receive intent
        if (Intent.ACTION_VIEW != intent.action) {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show()
            return
        }

        val url = intent.data.toString()

        val bundle = Bundle()
        bundle.putString("url", url)

        val fragmentTransaction = fragmentManager.beginTransaction()
        val optionFragment = OptionFragment()
        optionFragment.arguments = bundle
        fragmentTransaction.add(R.id.root_layout, optionFragment)
        fragmentTransaction.commit()

        PLVUrlService(this, this).requestGetPLVUrl(url)
    }

    override fun onStop() {
        super.onStop()

        // flash cache
        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
    }

    override fun onGetPLVUrlFinished(plvUrls: Array<PLVUrl>) {
        val bundle = Bundle()
        bundle.putBoolean("single_frag", true)
        bundle.putParcelable("plvurl", plvUrls[0])

        if (plvUrls.size() == 1) {
            if (plvUrls[0].isVideo) onVideoShowFragmentRequired(plvUrls[0])
            else onShowFragmentRequired(plvUrls[0])
        } else {
            isSingle = false
            scrollView!!.visibility = View.VISIBLE
            tileView!!.twitterViewListener = this
            tileView!!.imageLoader = ImageLoader(Volley.newRequestQueue(applicationContext), BitmapCache())

            plvUrls.forEach { plvUrl ->
                val position = tileView!!.addImageView()
                if (plvUrl.isVideo) tileView!!.setVideoUrl(position, plvUrl)
                else tileView!!.setImageUrl(position, plvUrl)
            }
        }
    }

    override fun onGetPLVUrlFailed(text: String) {
        Toast.makeText(this@Show, text, Toast.LENGTH_LONG).show()
    }

    override fun onURLAccepted() {

    }

    override fun hideProgressBar() {
        findViewById(R.id.show_progress).visibility = View.GONE
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
            fragment.arguments = bundle

            val fragmentTransaction = fragmentManager.beginTransaction()
            // back to this screen when back pressed
            if (!isSingle) fragmentTransaction.addToBackStack(null)
            fragmentTransaction.replace(R.id.show_frag_replace, fragment)
            fragmentTransaction.commit()

        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

    }
}
