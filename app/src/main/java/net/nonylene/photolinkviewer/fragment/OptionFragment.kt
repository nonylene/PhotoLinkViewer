package net.nonylene.photolinkviewer.fragment

import android.app.Dialog
import android.app.DialogFragment
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.Settings

class OptionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.option_fragment, container, false)

        val otherButtons = view.findViewById(R.id.buttons)

        view.findViewById(R.id.basebutton).setOnClickListener{ baseButton ->
            val rotateFlag = baseButton.getTag(R.id.ROTATE_FLAG_TAG) as Boolean? ?: false
            if (rotateFlag) {
                (baseButton as ImageButton).setImageResource(R.drawable.up_button_design)
                otherButtons.visibility = View.GONE
            } else {
                (baseButton as ImageButton).setImageResource(R.drawable.down_button_design)
                otherButtons.visibility = View.VISIBLE
            }
            baseButton.setTag(R.id.ROTATE_FLAG_TAG, !rotateFlag)
        }

        view.findViewById(R.id.setbutton).setOnClickListener{
            startActivity(Intent(activity, Settings::class.java))
        }

        view.findViewById(R.id.webbutton).setOnClickListener{
            // open intent chooser dialog
            IntentDialogFragment().apply {
                arguments = this@OptionFragment.arguments
            }.show(fragmentManager, "intent")
        }

        view.findViewById(R.id.rotate_rightbutton).setOnClickListener {
            rotateImg(true)
        }

        view.findViewById(R.id.rotate_leftbutton).setOnClickListener {
            rotateImg(false)
        }

        return view
    }

    private fun rotateImg(right: Boolean) {
        //get display size
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        val imageView = activity.findViewById(R.id.imgview) as ImageView
        imageView.imageMatrix = Matrix().apply {
            set(imageView.imageMatrix)
            postRotate(if (right) 90f else -90f, (size.x / 2).toFloat(), (size.y / 2).toFloat())
        }
    }

    class IntentDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
            // get uri from bundle
            val uri = Uri.parse(arguments.getString("url"))

            val packageManager = activity.packageManager
            // organize data and save to app class
            val appsList = packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), 0)
                    .map { Apps(it, packageManager) }

            val listAdapter = object : ArrayAdapter<Apps>(activity, android.R.layout.select_dialog_item, android.R.id.text1, appsList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
                    val view = super.getView(position, convertView, parent)
                    // get dp
                    val dp = resources.displayMetrics.density

                    // set size
                    val iconSize = (40 * dp).toInt()
                    val viewSize = (50 * dp).toInt()
                    val paddingSize = (15 * dp).toInt()

                    // resize app icon (bitmap_factory makes low-quality images)
                    val appIcon = appsList[position].icon.apply {
                        setBounds(0, 0, iconSize, iconSize)
                    }

                    (view.findViewById(android.R.id.text1) as TextView).apply {
                        // resize text size
                        textSize = 20f
                        // set app-icon and bounds
                        setCompoundDrawables(appIcon, null, null, null)
                        compoundDrawablePadding = paddingSize
                        // set textView-height
                        layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, viewSize)
                    }

                    return view
                }
            }

            // make alert from list-adapter
            return AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.intent_title))
                    .setAdapter(listAdapter, { dialogInterface, i->
                        // start activity
                        startActivity(Intent().apply {
                            val apps = appsList[i]
                            setClassName(apps.packageName, apps.className)
                        })
                    })
                    .create()
        }

        inner class Apps {
            // class to list-adapter
            public val icon: Drawable
            public val name: String
            public val packageName: String
            public val className: String

            constructor(resolveInfo: ResolveInfo, packageManager: PackageManager) {
                name = resolveInfo.loadLabel(packageManager).toString()
                icon = resolveInfo.loadIcon(packageManager)
                packageName = resolveInfo.activityInfo.packageName
                className = resolveInfo.activityInfo.name
            }

            override fun toString(): String {
                return name
            }
        }
    }
}
