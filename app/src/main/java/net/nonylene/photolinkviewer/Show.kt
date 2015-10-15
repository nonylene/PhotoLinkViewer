package net.nonylene.photolinkviewer

import android.app.Activity
import android.content.Intent
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import net.nonylene.photolinkviewer.fragment.OptionFragment
import net.nonylene.photolinkviewer.fragment.ShowFragment
import net.nonylene.photolinkviewer.fragment.VideoShowFragment
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.PLVUrlService
import net.nonylene.photolinkviewer.tool.ProgressBarListener

import java.io.File
import java.io.IOException

class Show : Activity(), PLVUrlService.PLVUrlListener, ProgressBarListener {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)

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

        try {
            val fragmentTransaction = fragmentManager.beginTransaction()

            if (plvUrls[0].isVideo) {
                val videoShowFragment = VideoShowFragment()
                videoShowFragment.arguments = bundle
                fragmentTransaction.replace(R.id.show_frag_replace, videoShowFragment)
            } else {
                val showFragment = ShowFragment()
                showFragment.arguments = bundle
                fragmentTransaction.replace(R.id.show_frag_replace, showFragment)
            }
            fragmentTransaction.commit()

        } catch (e: IllegalStateException) {
            e.printStackTrace()
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
}
