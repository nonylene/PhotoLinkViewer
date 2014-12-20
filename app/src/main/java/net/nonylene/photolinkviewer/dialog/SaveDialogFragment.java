package net.nonylene.photolinkviewer.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.R;

import java.io.File;

public class SaveDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String filename;
        final File dir;
        // get site and url from bundle
        final Bundle bundle = getArguments();
        final String sitename = bundle.getString("sitename");
        final String url = bundle.getString("file_url");
        // set download directory
        final String directory = preferences.getString("download_dir", "PLViewer");
        final File root = Environment.getExternalStorageDirectory();
        // set filename (follow setting)
        if (preferences.getString("download_file", "mkdir").equals("mkdir")) {
            // make directory
            dir = new File(root, directory + "/" + sitename);
            filename = bundle.getString("filename");
        } else {
            // not make directory
            dir = new File(root, directory);
            filename = sitename + "-" + bundle.getString("filename");
        }
        dir.mkdirs();
        // set custom view
        View view = View.inflate(getActivity(), R.layout.save_path, null);
        // set directory to text
        TextView textView = (TextView) view.findViewById(R.id.path_TextView);
        textView.setText(dir.toString());
        // set pre_name to edit_text
        EditText editText = (EditText) view.findViewById(R.id.path_EditText);
        editText.setText(filename + ".png");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle(getString(R.string.save_dialog_title))
                .setPositiveButton(getString(R.string.save_dialog_positive), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // get filename
                        EditText editText = (EditText) getDialog().findViewById(R.id.path_EditText);
                        String filename = editText.getText().toString();
                        File path = new File(dir, filename);
                        //save file
                        Uri uri = Uri.parse(url);
                        // use download manager
                        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        request.setDestinationUri(Uri.fromFile(path));
                        request.setTitle("PhotoLinkViewer");
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                        downloadManager.enqueue(request);
                        Toast.makeText(getActivity(), getString(R.string.download_photo_title) + path.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(getString(R.string.save_dialog_negative), null);
        return builder.create();
    }
}
