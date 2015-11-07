package net.nonylene.photolinkviewer.fragment

import android.content.Intent
import android.os.Bundle
import android.preference.ListPreference

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.dialog.BatchDialogFragment

class WifiFragment : PreferenceSummaryFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.quality_setting_wifi)
        // on click batch
        findPreference("quality_wifi_batch").setOnPreferenceClickListener {
            // batch dialog
            BatchDialogFragment().apply {
                setTargetFragment(this@WifiFragment, 1)
                show(this@WifiFragment.fragmentManager, "batch")
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // batch dialog listener
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> batchSelected(resultCode)
        }
    }

    private fun batchSelected(resultCode: Int) {
        // change preferences in a lump
        val flickrPreference = findPreference("flickr_quality_wifi") as ListPreference
        val twitterPreference = findPreference("twitter_quality_wifi") as ListPreference
        val twipplePreference = findPreference("twipple_quality_wifi") as ListPreference
        val imglyPreference = findPreference("imgly_quality_wifi") as ListPreference
        val instagramPreference = findPreference("instagram_quality_wifi") as ListPreference
        val nicoPreference = findPreference("nicoseiga_quality_wifi") as ListPreference
        val tumblrPreference = findPreference("tumblr_quality_wifi") as ListPreference

        when (resultCode) {
            0 -> {
                flickrPreference.value = "original"
                twitterPreference.value = "original"
                twipplePreference.value = "original"
                imglyPreference.value = "full"
                instagramPreference.value = "large"
                nicoPreference.value = "original"
                tumblrPreference.value = "original"
            }
            1 -> {
                flickrPreference.value = "large"
                twitterPreference.value = "large"
                twipplePreference.value = "large"
                imglyPreference.value = "large"
                instagramPreference.value = "large"
                nicoPreference.value = "large"
                tumblrPreference.value = "large"
            }
            2 -> {
                flickrPreference.value = "medium"
                twitterPreference.value = "medium"
                twipplePreference.value = "large"
                imglyPreference.value = "medium"
                instagramPreference.value = "medium"
                nicoPreference.value = "medium"
                tumblrPreference.value = "medium"
            }
        }
    }
}
