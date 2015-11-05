package net.nonylene.photolinkviewer.fragment

import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.R

import twitter4j.Status
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterMethod

class TwitterOptionFragment : Fragment() {

    companion object {
        private val LIKE_CODE = 1
        private val RETWEET_CODE = 2
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bundle = arguments
        // add twitter buttons
        val view = inflater.inflate(R.layout.twitter_option, container, false)

        view.findViewById(R.id.retweet_button).setOnClickListener{
            TwitterDialogFragment().apply {
                arguments = bundle
                setTargetFragment(this@TwitterOptionFragment, RETWEET_CODE)
                show(this@TwitterOptionFragment.fragmentManager, "retweet")
            }
        }

        view.findViewById(R.id.like_button).setOnClickListener{
            TwitterDialogFragment().apply {
                arguments = bundle
                setTargetFragment(this@TwitterOptionFragment, LIKE_CODE)
                show(this@TwitterOptionFragment.fragmentManager, "like")
            }
        }
        return view
    }

    class TwitterDialogFragment : DialogFragment() {
        private var requestCode: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // get request code
            requestCode = targetRequestCode
            // get twitter id
            val id_long = arguments.getLong("id_long")

            // get screen_name
            val screenName = activity.getSharedPreferences("preference", Context.MODE_PRIVATE)
                    .getString("screen_name", null)

            // get account_list
            val accountsList = MyAsyncTwitter.getAccountsList(activity)
            val screen_list = accountsList.screenList
            val row_id_list = accountsList.rowIdList
            // array_list to adapter
            val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, screen_list).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // get view
            val view = View.inflate(activity, R.layout.spinner_dialog, null)
            val textView = view.findViewById(R.id.spinner_text) as TextView
            val spinner = (view.findViewById(R.id.accounts_spinner) as Spinner).apply {
                setAdapter(adapter)
                setSelection(screen_list.indexOf(screenName))
            }

            val builder = AlertDialog.Builder(activity)
            // change behave for request code
            when (requestCode) {
                LIKE_CODE -> {
                    textView.text = getString(R.string.like_dialog_message)
                    builder.setTitle(getString(R.string.like_dialog_title))
                }
                RETWEET_CODE  -> {
                    textView.text = getString(R.string.retweet_dialog_message)
                    builder.setTitle(getString(R.string.retweet_dialog_title))
                }
            }

            return builder
                    .setView(view)
                    .setPositiveButton(getString(android.R.string.ok), { dialog, which ->
                        val row_id = row_id_list[spinner.selectedItemPosition]
                        targetFragment.onActivityResult(requestCode, row_id,
                                Intent().putExtra("id_long", id_long))
                    })
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .create()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        val applicationContext = activity.applicationContext
        // request code > like or retweet
        // result code > row_id
        // intent > id_long
        val twitter = MyAsyncTwitter.getAsyncTwitter(activity, resultCode).apply {
            addListener(object : TwitterAdapter() {
                override fun onException(e: TwitterException?, twitterMethod: TwitterMethod?) {
                    val message = "${getString(R.string.twitter_error_toast)}: ${e!!.statusCode}\n(${e.errorMessage})"

                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun createdFavorite(status: Status?) {
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(applicationContext, applicationContext.getString(R.string.twitter_like_toast), Toast.LENGTH_LONG).show()
                    }
                }

                override fun retweetedStatus(status: Status?) {
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(applicationContext, applicationContext.getString(R.string.twitter_retweet_toast), Toast.LENGTH_LONG).show()
                    }
                }
            })
        }

        val id_long = data.getLongExtra("id_long", -1)
        when (requestCode) {
            LIKE_CODE -> twitter.createFavorite(id_long)
            RETWEET_CODE  -> twitter.retweetStatus(id_long)
        }
    }
}
