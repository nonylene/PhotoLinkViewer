package net.nonylene.photolinkviewer

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.ActivityInstrumentationTestCase2

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaxSizePreferenceActivityTest : ActivityInstrumentationTestCase2<MaxSizePreferenceActivity>(MaxSizePreferenceActivity::class.java) {

    init {
        injectInstrumentation(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun finishTest() {
        activity.finish()
    }
}
