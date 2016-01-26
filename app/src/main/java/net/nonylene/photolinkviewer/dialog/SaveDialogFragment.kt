package net.nonylene.photolinkviewer.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.TextView

import net.nonylene.photolinkviewer.R

class SaveDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments
        // set custom view
        val view = View.inflate(activity, R.layout.save_path, null)
        (view.findViewById(R.id.path_TextView) as TextView).setText(arguments.getString("dir"))
        (view.findViewById(R.id.path_EditText) as EditText).setText(arguments.getString("filename"))
        return AlertDialog.Builder(activity).setView(view)
                .setTitle(getString(R.string.save_dialog_title))
                .setPositiveButton(getString(R.string.save_dialog_positive), { dialogInterface, i ->
                        // get filename
                        arguments.putString("filename",
                                (dialog.findViewById(R.id.path_EditText) as EditText).text.toString())
                        targetFragment.onActivityResult(targetRequestCode, 0, Intent().putExtra("bundle", arguments))
                    }
                )
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create()
    }
}
