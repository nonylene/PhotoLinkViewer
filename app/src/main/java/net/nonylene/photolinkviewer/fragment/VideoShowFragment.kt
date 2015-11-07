package net.nonylene.photolinkviewer.fragment

import android.app.Fragment
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.VideoView

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.tool.PLVUrl
import net.nonylene.photolinkviewer.tool.ProgressBarListener

class VideoShowFragment : Fragment() {
    private var baseView: View? = null
    private var videoShowFrameLayout: FrameLayout? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        baseView = inflater.inflate(R.layout.videoshow_fragment, container, false)
        videoShowFrameLayout = baseView!!.findViewById(R.id.videoshowframe) as FrameLayout
        progressBar = baseView!!.findViewById(R.id.show_progress) as ProgressBar
        if (arguments.getBoolean("single_frag", false)) {
            videoShowFrameLayout!!.setBackgroundResource(R.color.transparent)
            // do not hide progressbar! progressbar of activity will be displayed under videoView.
        }
        val plvUrl = arguments.getParcelable<PLVUrl>("plvurl")
        playVideo(plvUrl)
        return baseView
    }

    private fun playVideo(plvUrl: PLVUrl) {
        // view video
        val videoView = baseView!!.findViewById(R.id.videoview) as VideoView
        videoView.setVideoURI(Uri.parse(plvUrl.displayUrl))
        val mediaController = MediaController(activity)
        videoView.setMediaController(mediaController)
        // touch to stop
        videoView.setOnPreparedListener { mp ->
            //remove progressbar
            removeProgressBar()
            videoShowFrameLayout!!.setBackgroundColor(resources.getColor(R.color.background))
            val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
            videoView.setBackgroundColor(Color.TRANSPARENT)
            if (preferences.getBoolean("video_play", true)) {
                mp.start()
            } else {
                mediaController.show()
                mp.seekTo(1)
            }
        }
        
        videoView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (mediaController.isShowing) {
                    mediaController.hide()
                } else {
                    mediaController.show()
                }
            }
            true
        }

        // loop
        videoView.setOnCompletionListener { mp ->
            mp.start()
        }

        // prevent infinite loading
        videoView.setOnErrorListener { mediaPlayer, what, extra ->
            videoView.setOnCompletionListener(null)
            videoView.stopPlayback()
            false
        }
    }

    private fun removeProgressBar() {
        videoShowFrameLayout!!.removeView(progressBar)
        (activity as? ProgressBarListener)?.hideProgressBar()
    }
}
