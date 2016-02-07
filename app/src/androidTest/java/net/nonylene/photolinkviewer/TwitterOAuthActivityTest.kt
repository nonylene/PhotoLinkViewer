package net.nonylene.photolinkviewer

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TwitterOAuthActivityTest {

    @Rule @JvmField
    var activityTestRule = ActivityTestRule(TwitterOAuthActivity::class.java);

    @Test
    fun finishTest() {
        activityTestRule.activity.finish()
    }
}
