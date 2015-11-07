package net.nonylene.photolinkviewer.fragment

import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.preference.ListPreference
import android.support.v7.app.AlertDialog

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.dialog.BatchDialogFragment

class LTEFragment : PreferenceSummaryFragment() {
    internal var listener: OnWifiSwitchListener? = null

    // lister when switch changed
    interface OnWifiSwitchListener {
        fun onChanged(checked: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.quality_setting_3g)
        // on click batch
        findPreference("quality_3g_batch").setOnPreferenceClickListener {
            // batch dialog
            BatchDialogFragment().apply {
                setTargetFragment(this@LTEFragment, 1)
                show(this@LTEFragment.fragmentManager, "batch")
            }
            false
        }
        // on notes
        findPreference("quality_note").setOnPreferenceClickListener{
            NoteDialogFragment().apply {
                show(this@LTEFragment.fragmentManager, "batch")
            }
            false
        }
        // on switch changed
        findPreference("wifi_switch").setOnPreferenceChangeListener { preference, newValue ->
            listener?.onChanged(newValue.toString().toBoolean())
            true
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        listener = activity as OnWifiSwitchListener
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
        val flickrPreference = findPreference("flickr_quality_3g") as ListPreference
        val twitterPreference = findPreference("twitter_quality_3g") as ListPreference
        val twipplePreference = findPreference("twipple_quality_3g") as ListPreference
        val imglyPreference = findPreference("imgly_quality_3g") as ListPreference
        val instagramPreference = findPreference("instagram_quality_3g") as ListPreference
        val nicoPreference = findPreference("nicoseiga_quality_3g") as ListPreference
        val tumblrPreference = findPreference("tumblr_quality_3g") as ListPreference

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

    class NoteDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.notes_dialog_title))
                    .setMessage(getString(R.string.notes_about_quality))
                    .setPositiveButton(getString(android.R.string.ok), null)
                    .create()
    }
}
