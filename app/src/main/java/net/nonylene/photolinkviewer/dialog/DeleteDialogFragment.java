package net.nonylene.photolinkviewer.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import net.nonylene.photolinkviewer.R;

public class DeleteDialogFragment extends DialogFragment {
    private DeleteDialogCallBack deleteDialogCallBack;

    public interface DeleteDialogCallBack {
        void onDeleteConfirmed(String userName);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String screenName = getArguments().getString("screen_name");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.delete_dialog_title))
                .setMessage(getString(R.string.delete_dialog_message_account) + screenName + "\n" + getString(R.string.delete_dialog_message))
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (deleteDialogCallBack != null)
                            deleteDialogCallBack.onDeleteConfirmed(screenName);
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null);
        return builder.create();
    }

    public void setDeleteDialogCallBack(DeleteDialogCallBack deleteDialogCallBack) {
        this.deleteDialogCallBack = deleteDialogCallBack;
    }
}
