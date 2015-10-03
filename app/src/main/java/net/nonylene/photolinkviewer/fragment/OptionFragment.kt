package net.nonylene.photolinkviewer.fragment

import android.content.Intent
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.Settings

class OptionFragment : Fragment() {

    private var otherButtons : View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.option_fragment, container, false)

        view.findViewById(R.id.basebutton).setOnClickListener{ baseButton ->
            val rotateFlag = baseButton.getTag(R.id.ROTATE_FLAG_TAG) as Boolean? ?: false
            if (rotateFlag) {
                (baseButton as ImageButton).setImageResource(R.drawable.up_button_design)
                otherButtons!!.visibility = View.GONE
            } else {
                (baseButton as ImageButton).setImageResource(R.drawable.down_button_design)
                otherButtons!!.visibility = View.VISIBLE
            }
            baseButton.setTag(R.id.ROTATE_FLAG_TAG, !rotateFlag)
        }

        view.findViewById(R.id.setbutton).setOnClickListener{
            startActivity(Intent(activity, Settings::class.java))
        }

        view.findViewById(R.id.webbutton).setOnClickListener{
            // get uri from bundle
            val uri = Uri.parse(arguments.getString("url"))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            // open intent chooser
            startActivity(Intent.createChooser(intent, getString(R.string.intent_title)))
        }

        view.findViewById(R.id.rotate_rightbutton).setOnClickListener {
            rotateImg(true)
        }

        view.findViewById(R.id.rotate_leftbutton).setOnClickListener {
            rotateImg(false)
        }

        otherButtons = view.findViewById(R.id.buttons) as LinearLayout

        return view
    }

    private fun rotateImg(right: Boolean) {
        //get display size
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        val imageView = activity.findViewById(R.id.imgview) as ImageView
        val matrix = Matrix()
        matrix.set(imageView.imageMatrix)
        if (right) {
            matrix.postRotate(90f, (size.x / 2).toFloat(), (size.y / 2).toFloat())
        } else {
            matrix.postRotate(-90f, (size.x / 2).toFloat(), (size.y / 2).toFloat())
        }
        imageView.imageMatrix = matrix
    }
}
