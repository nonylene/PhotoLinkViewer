package net.nonylene.photolinkviewer.fragment

import android.app.DownloadManager
import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.content.SharedPreferences
import android.graphics.Matrix
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.Fragment
import android.os.Environment
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.async.AsyncHttpBitmap
import net.nonylene.photolinkviewer.dialog.SaveDialogFragment
import net.nonylene.photolinkviewer.tool.Initialize
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.ProgressBarListener

import java.io.File

class ShowFragment : Fragment() {

    private var baseView: View? = null
    private var imageView: ImageView? = null
    private var showFrameLayout: FrameLayout? = null
    private var progressBar: ProgressBar? = null

    private var preferences: SharedPreferences? = null
    private var firstzoom = 1f
    private var quickScale: MyQuickScale? = null
    private var applicationContext : Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        applicationContext = activity.applicationContext

        preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        baseView = inflater.inflate(R.layout.show_fragment, container, false)

        imageView = baseView!!.findViewById(R.id.imgview) as ImageView
        showFrameLayout = baseView!!.findViewById(R.id.showframe) as FrameLayout
        progressBar = baseView!!.findViewById(R.id.showprogress) as ProgressBar

        val scaleGestureDetector = ScaleGestureDetector(activity, simpleOnScaleGestureListener())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scaleGestureDetector.isQuickScaleEnabled = false
        }

        val gestureDetector = GestureDetector(activity, simpleOnGestureListener())

        imageView!!.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }
            when (event.action) {
            // image_view double_tap quick scale
                MotionEvent.ACTION_MOVE -> quickScale?.onMove(event)
                MotionEvent.ACTION_UP   -> quickScale = null
            }
            true
        }

        if (!preferences!!.getBoolean("initialized39", false)) {
            Initialize.initialize39(activity)
        }

        if (arguments.getBoolean("single_frag", false)) {
            showFrameLayout!!.setBackgroundResource(R.color.transparent)
            progressBar!!.visibility = View.GONE
        }

        AsyncExecute(arguments.getParcelable<PLVUrl>("plvurl")).Start()

        return baseView
    }

    internal inner class simpleOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        var double_zoom: Boolean = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // drag photo
            val matrix = Matrix()
            matrix.set(imageView!!.imageMatrix)
            val values = FloatArray(9)
            matrix.getValues(values)
            // move photo
            values[Matrix.MTRANS_X] = values[Matrix.MTRANS_X] - distanceX
            values[Matrix.MTRANS_Y] = values[Matrix.MTRANS_Y] - distanceY
            matrix.setValues(values)
            imageView!!.imageMatrix = matrix
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            double_zoom = preferences!!.getBoolean("double_zoom", false)
            quickScale = MyQuickScale(e, !double_zoom)
            if (!double_zoom) doubleZoom(e)
            return super.onDoubleTap(e)
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) {
                quickScale?.let {
                    if (double_zoom && !it.moved()) doubleZoom(e)
                    quickScale = null
                }
            }
            return false
        }

        private fun doubleZoom(e: MotionEvent) {
            val touchX = e.x
            val touchY = e.y
            imageView!!.startAnimation(ScaleAnimation(1f, 2f, 1f, 2f, touchX, touchY).apply {
                duration = 200
                isFillEnabled = true
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        val matrix = Matrix()
                        matrix.set(imageView!!.imageMatrix)
                        matrix.postScale(2f, 2f, touchX, touchY)
                        imageView!!.imageMatrix = matrix
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                    }
                })
            })
        }
    }

    private inner class MyQuickScale(e: MotionEvent, double_frag: Boolean) {
        // quick scale zoom
        private val initialY: Float
        private val initialX: Float
        private var basezoom: Float = 0f
        private var old_zoom: Float = 0f
        private var moved = false

        init {
            initialY = e.y
            initialX = e.x
            //get current status
            val values = FloatArray(9)
            val matrix = Matrix()
            matrix.set(imageView!!.imageMatrix)
            matrix.getValues(values)
            //set base zoom param
            basezoom = values[Matrix.MSCALE_X]
            if (basezoom == 0f) basezoom = Math.abs(values[Matrix.MSKEW_X])
            // double tap
            if (double_frag) basezoom *= 2
            old_zoom = 1f
        }

        fun onMove(e: MotionEvent) {
            moved = true
            val touchY = e.y
            val matrix = Matrix()
            matrix.set(imageView!!.imageMatrix)
            // adjust zoom speed
            // If using preference_fragment, value is saved to DefaultSharedPref.
            val zoomSpeed = (preferences!!.getString("zoom_speed", "1.4")).toFloat()
            val new_zoom = Math.pow((touchY / initialY).toDouble(), (zoomSpeed * 2).toDouble()).toFloat()
            // photo's zoom scale (is relative to old zoom value.)
            val scale = new_zoom / old_zoom
            if (new_zoom > firstzoom / basezoom * 0.8) {
                old_zoom = new_zoom
                matrix.postScale(scale, scale, initialX, initialY)
                imageView!!.imageMatrix = matrix
            }
        }

        fun moved(): Boolean {
            return moved
        }
    }

    internal inner class simpleOnScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var touchX: Float = 0f
        private var touchY: Float = 0f
        private var basezoom: Float = 0f
        private var old_zoom: Float = 0f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            //define zoom-base point
            touchX = detector.focusX
            touchY = detector.focusY
            //get current status
            val values = FloatArray(9)
            val matrix = Matrix()
            matrix.set(imageView!!.imageMatrix)
            matrix.getValues(values)
            //set base zoom param
            basezoom = values[Matrix.MSCALE_X]
            if (basezoom == 0f) basezoom = Math.abs(values[Matrix.MSKEW_X])
            old_zoom = 1f
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val matrix = Matrix()
            matrix.set(imageView!!.imageMatrix)
            // adjust zoom speed
            // If using preference_fragment, value is saved to DefaultSharedPref.
            val zoomSpeed = (preferences!!.getString("zoom_speed", "1.4")).toFloat()
            val new_zoom = Math.pow(detector.scaleFactor.toDouble(), zoomSpeed.toDouble()).toFloat()
            // photo's zoom scale (is relative to old zoom value.)
            val scale = new_zoom / old_zoom
            if (new_zoom > firstzoom / basezoom * 0.8) {
                old_zoom = new_zoom
                matrix.postScale(scale, scale, touchX, touchY)
                imageView!!.imageMatrix = matrix
            }
            return super.onScale(detector)
        }
    }

    inner class AsyncExecute(private val plvUrl: PLVUrl) : LoaderManager.LoaderCallbacks<AsyncHttpBitmap.Result> {

        fun Start() {
            //there are some loaders, so restart(all has finished)
            val bundle = Bundle().apply {
                putParcelable("plvurl", plvUrl)
            }
            loaderManager.restartLoader(0, bundle, this)
        }

        override fun onCreateLoader(id: Int, bundle: Bundle): Loader<AsyncHttpBitmap.Result> {
            val max_size = 2048
            return AsyncHttpBitmap(activity.applicationContext, bundle.getParcelable<PLVUrl>("plvurl"), max_size)
        }

        override fun onLoadFinished(loader: Loader<AsyncHttpBitmap.Result>, result: AsyncHttpBitmap.Result) {
            plvUrl.type = result.type
            plvUrl.height = result.originalHeight
            plvUrl.width = result.originalWidth
            addDLButton(plvUrl)

            if ("gif" == result.type) {
                addWebView(plvUrl)
            } else {
                removeProgressBar()
            }

            val bitmap = result.bitmap

            val display = activity.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val dispWidth = size.x
            val dispHeight = size.y

            if (bitmap == null) {
                Toast.makeText(baseView!!.context, getString(R.string.show_bitamap_error) +
                        result.errorMessage?.let { "\n" + it }, Toast.LENGTH_LONG).show()
                return
            }

            //get bitmap size
            val origWidth = bitmap.width.toFloat()
            val origHeight = bitmap.height.toFloat()
            //set image
            imageView!!.setImageBitmap(bitmap)
            //get matrix from imageview
            val matrix = Matrix()
            matrix.set(imageView!!.matrix)
            //get display size
            val wid = dispWidth / origWidth
            val hei = dispHeight / origHeight
            val zoom = Math.min(wid, hei)
            val initX: Float
            val initY: Float
            if (preferences!!.getBoolean("adjust_zoom", false) || zoom < 1) {
                //zoom
                matrix.setScale(zoom, zoom)
                if (wid < hei) {
                    //adjust width
                    initX = 0f
                    initY = (dispHeight - origHeight * wid) / 2
                } else {
                    //adjust height
                    initX = (dispWidth - origWidth * hei) / 2
                    initY = 0f
                }
                if (zoom < 1) {
                    firstzoom = zoom
                }
            } else {
                //move
                initX = (dispWidth - origWidth) / 2
                initY = (dispHeight - origHeight) / 2
            }

            matrix.postTranslate(initX, initY)
            imageView!!.imageMatrix = matrix

            activity.findViewById(R.id.rotate_root).visibility = View.VISIBLE

            if (result.isResized) {
                Toast.makeText(baseView!!.context,
                        getString(R.string.resize_message) + result.originalWidth + "x" + result.originalHeight,
                        Toast.LENGTH_LONG).show()
            }
        }

        override fun onLoaderReset(loader: Loader<AsyncHttpBitmap.Result>) {

        }
    }

    override fun onDetach() {
        super.onDetach()
        imageView?.setImageBitmap(null)
        activity.findViewById(R.id.dlbutton)?.let {
            activity.findViewById(R.id.dlbutton_frame)?.visibility = View.GONE
        }
        activity.findViewById(R.id.rotate_root)?.let {
            it.visibility = View.GONE
        }
    }

    private fun getFileNames(plvUrl: PLVUrl): Bundle {
        val dir: File
        // get site, url and type from bundle
        val siteName = plvUrl.siteName
        // set download directory
        val directory = preferences!!.getString("download_dir", "PLViewer")
        val root = Environment.getExternalStorageDirectory()
        // set filename (follow setting)
        var filename: String
        if (preferences!!.getString("download_file", "mkdir") == "mkdir") {
            // make directory
            dir = File(root, directory + "/" + siteName)
            filename = plvUrl.fileName
        } else {
            // not make directory
            dir = File(root, directory)
            filename = siteName + "-" + plvUrl.fileName
        }
        filename += "." + plvUrl.type
        dir.mkdirs()

        val bundle = Bundle().apply {
            putString("filename", filename)
            putString("dir", dir.toString())
        }

        //check wifi connecting and setting or not
        var wifi = false
        if (preferences!!.getBoolean("wifi_switch", false)) {
            // get wifi status
            val manager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            wifi = manager.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }

        val original = if (wifi) {
            preferences!!.getBoolean("original_switch_wifi", false)
        } else {
            preferences!!.getBoolean("original_switch_3g", false)
        }

        if (original) {
            bundle.putString("original_url", plvUrl.biggestUrl)
        } else {
            bundle.putString("original_url", plvUrl.displayUrl)
        }

        return bundle
    }

    private fun save(bundle: Bundle) {
        val uri = Uri.parse(bundle.getString("original_url"))
        val filename = bundle.getString("filename")
        val path = File(bundle.getString("dir"), filename)
        //save file
        // use download manager
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
                .setDestinationUri(Uri.fromFile(path))
                .setTitle("PhotoLinkViewer")
                .setDescription(filename)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        // notify
        if (preferences!!.getBoolean("leave_notify", true)) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        downloadManager.enqueue(request)
        Toast.makeText(applicationContext!!, applicationContext!!.getString(R.string.download_photo_title) + path.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0 -> save(data.getBundleExtra("bundle"))
        }
    }

    private fun addDLButton(plvUrl: PLVUrl) {
        // dl button visibility and click
        val dlButton = activity.findViewById(R.id.dlbutton) as ImageButton

        dlButton.setOnClickListener{
            // download direct
            if (preferences!!.getBoolean("skip_dialog", false)) {
                save(getFileNames(plvUrl))
            } else {
                // open dialog
                SaveDialogFragment().apply {
                    arguments = getFileNames(plvUrl)
                    setTargetFragment(this@ShowFragment, 0)
                    show(this@ShowFragment.fragmentManager, "Save")
                }
            }
        }

        activity.findViewById(R.id.dlbutton_frame).visibility = View.VISIBLE
    }

    private fun addWebView(plvUrl: PLVUrl) {
        val videoWidth = plvUrl.width
        val videoHeight = plvUrl.height

        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val dispWidth = size.x
        val dispHeight = size.y

        // gif view by web view
        val webView = WebView(activity).apply {
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }

        val layoutParams: FrameLayout.LayoutParams
        val escaped = TextUtils.htmlEncode(plvUrl.displayUrl)
        val html: String
        if ((videoHeight > dispHeight * 0.9 && videoWidth * dispHeight / dispHeight < dispWidth) || dispWidth * videoHeight / videoWidth > dispHeight) {
            // if height of video > disp_height * 0.9, check whether calculated width > disp_width . if this is true,
            // give priority to width. and, if check whether calculated height > disp_height, give priority to height.
            val width = (dispWidth * 0.9).toInt()
            val height = (dispHeight * 0.9).toInt()
            layoutParams = FrameLayout.LayoutParams(width, height)
            html = "<html><body><img style='display: block; margin: 0 auto' height='100%'src='$escaped'></body></html>"
        } else {
            val width = (dispWidth * 0.9).toInt()
            layoutParams = FrameLayout.LayoutParams(width, width * videoHeight / videoWidth)
            html = "<html><body><img style='display: block; margin: 0 auto' width='100%'src='$escaped'></body></html>"
        }
        layoutParams.gravity = Gravity.CENTER

        webView.apply {
            setLayoutParams(layoutParams)
            // html to centering
            loadData(html, "text/html", "utf-8")
            setBackgroundColor(0)
            settings.builtInZoomControls = true
        }

        removeProgressBar()
        showFrameLayout!!.addView(webView)
    }

    private fun removeProgressBar() {
        showFrameLayout!!.removeView(progressBar)
        (activity as? ProgressBarListener)?.hideProgressBar()
    }
}