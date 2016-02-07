package net.nonylene.photolinkviewer

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import net.nonylene.photolinkviewer.core.fragment.PreferenceSummaryFragment
import net.nonylene.photolinkviewer.core.tool.Initialize
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.tool.PLVUtils

import twitter4j.Status
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterMethod

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("initialized39", false)) {
            Initialize.initialize39(this)
        }
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceSummaryFragment() {

        private var instagramSwitch: SwitchPreference? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            addPreferencesFromResource(R.xml.settings)
            val aboutAppPreference = findPreference("about_app_preference")
            aboutAppPreference.setOnPreferenceClickListener {
                AboutDialogFragment().apply {
                    setTargetFragment(this@SettingsFragment, ABOUT_FRAGMENT)
                }.show(fragmentManager, "about")
                false
            }

            val instagramPreference = findPreference("instagram_preference")
            instagramPreference.setOnPreferenceClickListener {
                startActivity(Intent(activity, IOAuthActivity::class.java))
                false
            }

            instagramSwitch = findPreference("instagram_api") as SwitchPreference

            instagramSwitch!!.setOnPreferenceChangeListener { preference, newValue ->
                PLVUtils.refreshInstagramToken(activity, newValue as Boolean)
                true
            }

            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onResume() {
            super.onResume()
            // IOAuthActivity
            instagramSwitch!!.isEnabled = activity.getSharedPreferences("preference", Context.MODE_PRIVATE).getBoolean("instagram_authorized", false)
            instagramSwitch!!.isChecked = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("instagram_api", false)
        }

        // license etc
        class AboutDialogFragment : DialogFragment() {
            private var count = 0

            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                val builder = AlertDialog.Builder(activity)
                val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
                val view = View.inflate(activity, R.layout.about_app, null)
                with(view.findViewById(R.id.about_version) as TextView) {
                    append(info.versionName)
                    setOnClickListener {
                        count++
                        if (count == 5) {
                            targetFragment.onActivityResult(targetRequestCode, TWEET_CODE, null)
                            count = 0
                        }
                    }
                }

                return builder.setView(view)
                        .setTitle(getString(R.string.about_app_dialogtitle))
                        .setPositiveButton(getString(android.R.string.ok), null)
                        .create()
            }
        }

        class TwitterDialogFragment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                // get screen_name
                val sharedPreferences = activity.getSharedPreferences("preference", Context.MODE_PRIVATE)
                val screenName = sharedPreferences.getString("screen_name", null)

                // get account_list
                val accountsList = MyAsyncTwitter.getAccountsList(activity)
                val screen_list = accountsList.screenList
                val row_id_list = accountsList.rowIdList
                // array_list to adapter
                val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, screen_list).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                // get view
                val view = View.inflate(activity, R.layout.spinner_tweet, null)
                val spinner = (view.findViewById(R.id.accounts_spinner) as Spinner).apply {
                    setAdapter(adapter)
                    setSelection(screen_list.indexOf(screenName))
                }

                return AlertDialog.Builder(activity)
                        .setTitle(getString(R.string.tweet))
                        .setView(view)
                        .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                            val row_id = row_id_list[spinner.selectedItemPosition]
                            val editedText = getDialog().findViewById(R.id.spinner_edit) as EditText
                            targetFragment.onActivityResult(targetRequestCode, row_id,
                                    Intent().putExtra("tweet_text", editedText.text.toString()))
                        }
                        .setNegativeButton(getString(android.R.string.cancel), null)
                        .create()
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == ABOUT_FRAGMENT && resultCode == TWEET_CODE) {
                TwitterDialogFragment().apply {
                    setTargetFragment(this@SettingsFragment, TWITTER_FRAGMENT)
                }.show(fragmentManager, "twitter")
            } else if (requestCode == TWITTER_FRAGMENT) {
                // result code > row_id
                // intent > tweet_text
                val twitter = (MyAsyncTwitter.getAsyncTwitter(activity, resultCode)).apply {
                    addListener(object : TwitterAdapter() {

                        override fun onException(e: TwitterException, twitterMethod: TwitterMethod?) {
                            val message = getString(R.string.twitter_error_toast) + ": " + e.statusCode + "\n(" + e.errorMessage + ")"
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun updatedStatus(status: Status?) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(activity, getString(R.string.twitter_tweet_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    })
                }

                twitter.updateStatus(data!!.getStringExtra("tweet_text"))
            }

            super.onActivityResult(requestCode, resultCode, data)
        }

        companion object {
            private val ABOUT_FRAGMENT = 100
            private val TWITTER_FRAGMENT = 200
            private val TWEET_CODE = 10
        }
    }
}