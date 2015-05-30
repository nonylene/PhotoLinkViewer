package net.nonylene.photolinkviewer.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import net.nonylene.photolinkviewer.R;

public class SaveDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        // set custom view
        View view = View.inflate(getActivity(), R.layout.save_path, null);
        // set directory to text
        TextView textView = (TextView) view.findViewById(R.id.path_TextView);
        textView.setText(bundle.getString("dir"));
        // set pre_name to edit_text
        EditText editText = (EditText) view.findViewById(R.id.path_EditText);
        editText.setText(bundle.getString("filename"));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle(getString(R.string.save_dialog_title))
                .setPositiveButton(getString(R.string.save_dialog_positive), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // get filename
                        EditText editText = (EditText) getDialog().findViewById(R.id.path_EditText);
                        bundle.putString("filename",editText.getText().toString());
                        Intent intent = new Intent();
                        intent.putExtra("bundle",bundle);
                        getTargetFragment().onActivityResult(0, 0, intent);
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null);
        return builder.create();
    }
}
