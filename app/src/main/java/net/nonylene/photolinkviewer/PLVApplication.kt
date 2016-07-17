package net.nonylene.photolinkviewer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.squareup.okhttp.Cache
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import io.fabric.sdk.android.Fabric
import io.fabric.sdk.android.Kit
import net.nonylene.photolinkviewer.core.PhotoLinkViewer
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.tool.PLVUtils
import net.nonylene.photolinkviewer.tool.migrate52
import java.io.File
import java.io.IOException

class PLVApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var cache: Cache? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        MyAsyncTwitter.createTwitterTable(this)

        Fabric.with(this,
                *arrayOf<Kit<*>>(Twitter(TwitterAuthConfig(BuildConfig.TWITTER_KEY, BuildConfig.TWITTER_SECRET)))
                        .let { if (BuildConfig.IS_CRASHLYTICS_ENABLED) it.plus(Crashlytics()) else it }
        )

        PhotoLinkViewer.with(PhotoLinkViewer.TwitterKeys(BuildConfig.TWITTER_KEY, BuildConfig.TWITTER_SECRET),
                BuildConfig.FLICKR_KEY, BuildConfig.TUMBLR_KEY, Settings::class.java)

        try {
            val cacheDir = File(applicationContext.cacheDir, "okhttp")
            cache = Cache(cacheDir, 20 * 1024 * 1024) // 20 MB
            PhotoLinkViewer.cache = cache
        } catch (e: IOException) {
            e.printStackTrace()
        }
        PLVUtils.refreshTwitterTokens(this)

        // migration
        migrate52(this)
    }

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity?) {
        cache?.flush()
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }
}

