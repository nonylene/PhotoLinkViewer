package net.nonylene.photolinkviewer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import io.fabric.sdk.android.Fabric
import io.fabric.sdk.android.Kit
import net.nonylene.photolinkviewer.tool.OkHttpManager

public class PLVApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        Fabric.with(this,
                *arrayOf<Kit<*>>(Twitter(TwitterAuthConfig(BuildConfig.TWITTER_KEY, BuildConfig.TWITTER_SECRET)))
                    .let { if (BuildConfig.IS_CRASHLYTICS_ENABLED) it.plus(Crashlytics()) else it }
        )
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
        OkHttpManager.flushCache()
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }
}

