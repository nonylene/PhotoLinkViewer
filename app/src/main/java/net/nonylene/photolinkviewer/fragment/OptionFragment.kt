package net.nonylene.photolinkviewer.fragment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.app.DialogFragment
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v13.app.FragmentCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.bindView
import de.greenrobot.event.EventBus

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.Settings
import net.nonylene.photolinkviewer.dialog.SaveDialogFragment
import net.nonylene.photolinkviewer.event.DownloadButtonEvent
import net.nonylene.photolinkviewer.event.RotateEvent
import net.nonylene.photolinkviewer.event.ShowFragmentEvent
import net.nonylene.photolinkviewer.event.SnackbarEvent
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.tool.PLVUrl
import twitter4j.Status
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterMethod
import java.io.File

class OptionFragment : Fragment() {

    private val baseView: CoordinatorLayout by bindView(R.id.option_base_view)
    private val baseButton: FloatingActionButton by bindView(R.id.basebutton)
    private val settingButton: FloatingActionButton by bindView(R.id.setbutton)
    private val webButton: FloatingActionButton by bindView(R.id.webbutton)
    private val downLoadButton: FloatingActionButton by bindView(R.id.dlbutton)
    private val rotateLeftButton: FloatingActionButton by bindView(R.id.rotate_leftbutton)
    private val rotateRightButton: FloatingActionButton by bindView(R.id.rotate_rightbutton)
    private val retweetButton: FloatingActionButton by bindView(R.id.retweet_button)
    private val likeButton: FloatingActionButton by bindView(R.id.like_button)

    private var saveBundle: Bundle? = null
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }
    private var applicationContext : Context? = null
    private val fastOutSlowInInterpolator = FastOutSlowInInterpolator()

    private var isDownloadEnabled = false
    private var isOpen = false

    // by default, animation does not run if not isLaidOut().
    private fun FloatingActionButton.showWithAnimation() {
        alpha = 0f
        scaleY = 0f
        scaleX = 0f
        animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(fastOutSlowInInterpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        visibility = View.VISIBLE
                    }
                })
    }

    companion object {
        private val LIKE_CODE = 1
        private val RETWEET_CODE = 2
        private val STORAGE_PERMISSION_REQUEST = 3
        private val SAVE_DIALOG_CODE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext = activity.applicationContext
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.option_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseButton.setOnClickListener{ baseButton ->
            settingButton.animate().cancel()
            webButton.animate().cancel()
            retweetButton.animate().cancel()
            likeButton.animate().cancel()
            downLoadButton.animate().cancel()
            settingButton.animate().cancel()

            if (isOpen) {
                ViewCompat.setRotation(baseButton, 180f)
                ViewCompat.animate(baseButton).rotationBy(180f).setDuration(150)
                settingButton.hide()
                webButton.hide()
                if (arguments.isTwitterEnabled()) {
                    retweetButton.hide()
                    likeButton.hide()
                }
                if (isDownloadEnabled) downLoadButton.hide()
            } else {
                ViewCompat.setRotation(baseButton, 0f)
                ViewCompat.animate(baseButton).rotationBy(180f).setDuration(150)
                settingButton.showWithAnimation()
                webButton.showWithAnimation()
                if (arguments.isTwitterEnabled()) {
                    retweetButton.showWithAnimation()
                    likeButton.showWithAnimation()
                }
                if (isDownloadEnabled) downLoadButton.showWithAnimation()
            }
            isOpen = !isOpen
        }

        settingButton.setOnClickListener{
            startActivity(Intent(activity, Settings::class.java))
        }

        webButton.setOnClickListener{
            // get uri from bundle
            val uri = Uri.parse(arguments.getString("url"))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            // open intent chooser
            startActivity(Intent.createChooser(intent, getString(R.string.intent_title)))
        }

        rotateRightButton.setOnClickListener {
            EventBus.getDefault().post(RotateEvent(true))
        }

        rotateLeftButton.setOnClickListener {
            EventBus.getDefault().post(RotateEvent(false))
        }

        val bundle = arguments

        if (bundle.isTwitterEnabled()) {
            retweetButton.setOnClickListener{
                TwitterDialogFragment().apply {
                    arguments = bundle
                    setTargetFragment(this@OptionFragment, RETWEET_CODE)
                    show(this@OptionFragment.fragmentManager, "retweet")
                }
            }

            likeButton.setOnClickListener{
                TwitterDialogFragment().apply {
                    arguments = bundle
                    setTargetFragment(this@OptionFragment, LIKE_CODE)
                    show(this@OptionFragment.fragmentManager, "like")
                }
            }
        }
    }

    class TwitterDialogFragment : DialogFragment() {
        private var requestCode: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // get request code
            requestCode = targetRequestCode
            // get twitter id
            val id_long = arguments.getTweetId()

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

    // eventBus catch event
    public fun onEvent(downloadButtonEvent: DownloadButtonEvent) {
        isDownloadEnabled = true
        addDLButton(downloadButtonEvent.plvUrl)
    }

    // eventBus catch event
    public fun onEvent(showFragmentEvent: ShowFragmentEvent) {
        rotateLeftButton.animate().cancel()
        rotateRightButton.animate().cancel()
        if (showFragmentEvent.isToBeShown) {
            rotateRightButton.showWithAnimation()
            rotateLeftButton.showWithAnimation()
        } else {
            isDownloadEnabled = false
            removeDLButton()
            rotateRightButton.hide()
            rotateLeftButton.hide()
        }
    }

    // eventBus catch event
    public fun onEvent(snackbarEvent: SnackbarEvent) {
        Snackbar.make(baseView, snackbarEvent.message, Snackbar.LENGTH_LONG).apply {
            snackbarEvent.actionMessage?.let {
                setAction(it, snackbarEvent.actionListener)
            }
        }.show()
    }

    private fun addDLButton(plvUrl: PLVUrl) {
        // dl button visibility and click
        downLoadButton.setOnClickListener{
            // download direct
            if (preferences.getBoolean("skip_dialog", false)) {
                saveOrRequestPermission(getFileNames(plvUrl))
            } else {
                // open dialog
                SaveDialogFragment().apply {
                    arguments = getFileNames(plvUrl)
                    setTargetFragment(this@OptionFragment, SAVE_DIALOG_CODE)
                    show(this@OptionFragment.fragmentManager, "Save")
                }
            }
        }
        downLoadButton.animate().cancel()
        if (isOpen) downLoadButton.showWithAnimation()
    }

    private fun removeDLButton() {
        // dl button visibility and click
        downLoadButton.setOnClickListener(null)
        downLoadButton.animate().cancel()
        if (isOpen) downLoadButton.hide()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST -> {
                if (grantResults?.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
                    save(saveBundle!!)
                } else {
                    Toast.makeText(applicationContext!!, applicationContext!!.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveOrRequestPermission(bundle: Bundle) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            saveBundle = bundle
            FragmentCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST)
        } else {
            save(bundle)
        }
    }

    private fun save(bundle: Bundle) {
        val uri = Uri.parse(bundle.getString("original_url"))
        val filename = bundle.getString("filename")
        val path = File(bundle.getString("dir"), filename)
        // save file
        val request = DownloadManager.Request(uri)
                .setDestinationUri(Uri.fromFile(path))
                .setTitle("PhotoLinkViewer")
                .setDescription(filename)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        // notify
        if (preferences.getBoolean("leave_notify", true)) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        (activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(applicationContext!!, applicationContext!!.getString(R.string.download_photo_title) + path.toString(),
                Toast.LENGTH_LONG).show()
    }

    private fun getFileNames(plvUrl: PLVUrl): Bundle {
        // set download directory
        val directory = preferences.getString("download_dir", "PLViewer")
        val root = Environment.getExternalStorageDirectory()

        val dir: File
        var filename: String
        if (preferences.getString("download_file", "mkdir") == "mkdir") {
            // make directory
            dir = File(root, directory + "/" + plvUrl.siteName)
            filename = plvUrl.fileName
        } else {
            // not make directory
            dir = File(root, directory)
            filename = plvUrl.siteName + "-" + plvUrl.fileName
        }
        filename += "." + plvUrl.type
        dir.mkdirs()

        val bundle = Bundle().apply {
            putString("filename", filename)
            putString("dir", dir.toString())
        }

        //check wifi connecting and setting or not
        var wifi = false
        if (preferences.getBoolean("wifi_switch", false)) {
            // get wifi status
            val manager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            wifi = manager.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }

        val original = if (wifi) {
            preferences.getBoolean("original_switch_wifi", false)
        } else {
            preferences.getBoolean("original_switch_3g", false)
        }

        bundle.putString("original_url", if (original) plvUrl.biggestUrl else plvUrl.displayUrl)

        return bundle
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SAVE_DIALOG_CODE) {
            saveOrRequestPermission(data!!.getBundleExtra("bundle"))
        } else {
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
            val id_long = data!!.getLongExtra("id_long", -1)
            when (requestCode) {
                LIKE_CODE -> twitter.createFavorite(id_long)
                RETWEET_CODE  -> twitter.retweetStatus(id_long)
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}

private val TWITTER_ENABLED_KEY = "is_twitter_enabled"
private val TWITTER_ID_KEY = "twitter_id_long"

fun Bundle.setTwitterEnabled(isTwitterEnabled: Boolean) {
    putBoolean(TWITTER_ENABLED_KEY, isTwitterEnabled)
}

fun Bundle.isTwitterEnabled() : Boolean {
    return getBoolean(TWITTER_ENABLED_KEY, false)
}

fun Bundle.setTweetId(tweetId: Long) {
    putLong(TWITTER_ID_KEY, tweetId)
}

fun Bundle.getTweetId() : Long {
    return getLong(TWITTER_ID_KEY)
}
