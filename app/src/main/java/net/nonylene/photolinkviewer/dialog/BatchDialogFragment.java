package net.nonylene.photolinkviewer.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import net.nonylene.photolinkviewer.R;

public class BatchDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] items = getResources().getStringArray(R.array.quality);
        builder.setTitle(getString(R.string.quality_setting_dialogtitle))
                .setNegativeButton(getString(android.R.string.cancel), null)
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // go fragment
                        getTargetFragment().onActivityResult(getTargetRequestCode(), which, null);
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

}
