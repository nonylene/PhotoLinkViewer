package net.nonylene.photolinkviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OptionFragment extends Fragment {
    private View view;
    private Boolean open = false;
    private ImageButton baseButton;
    private ImageButton dlButton;
    private ImageButton setButton;
    private ImageButton webButton;
    private ImageButton rotateRButton;
    private ImageButton rotateLButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.option_fragment, container, false);
        baseButton = (ImageButton) view.findViewById(R.id.basebutton);
        dlButton = (ImageButton) view.findViewById(R.id.dlbutton);
        setButton = (ImageButton) view.findViewById(R.id.setbutton);
        webButton = (ImageButton) view.findViewById(R.id.webbutton);
        rotateRButton = (ImageButton) view.findViewById(R.id.rotate_rightbutton);
        rotateLButton = (ImageButton) view.findViewById(R.id.rotate_leftbutton);
        baseButton.setOnClickListener(new BaseButtonClickListener());
        dlButton.setOnClickListener(new DlButtonClickListener());
        setButton.setOnClickListener(new SetButtonClickListener());
        webButton.setOnClickListener(new WebButtonClickListener());
        rotateRButton.setOnClickListener(new RotateRButtonClickListener());
        rotateLButton.setOnClickListener(new RotateLButtonClickListener());
        return view;
    }

    class BaseButtonClickListener implements View.OnClickListener {

        public void onClick(View v) {
            if (open) {
                baseButton.setImageResource(R.drawable.up_button_design);
                dlButton.setVisibility(View.GONE);
                setButton.setVisibility(View.GONE);
                webButton.setVisibility(View.GONE);
                open = false;
            } else {
                baseButton.setImageResource(R.drawable.down_button_design);
                dlButton.setVisibility(View.VISIBLE);
                setButton.setVisibility(View.VISIBLE);
                webButton.setVisibility(View.VISIBLE);
                open = true;
            }
        }
    }

    class DlButtonClickListener implements View.OnClickListener {

        public void onClick(View v) {
            // open dialog
            DialogFragment dialogFragment = new SaveDialogFragment();
            dialogFragment.setArguments(getArguments());
            dialogFragment.show(getFragmentManager(), "Save");
        }
    }

    class SetButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //settings
            Intent intent = new Intent(getActivity(), Settings.class);
            startActivity(intent);
        }
    }

    class WebButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            // show share dialog
            DialogFragment dialogFragment = new IntentDialogFragment();
            dialogFragment.setArguments(getArguments());
            dialogFragment.show(getFragmentManager(), "Intent");
        }

    }

    public static class IntentDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // get uri from bundle
            Uri uri = Uri.parse(getArguments().getString("url"));
            // receive intent list
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            List<ResolveInfo> resolveInfoList = getActivity().getPackageManager().queryIntentActivities(intent, 0);
            // organize data and save to app class
            final List<Apps> appsList = new ArrayList<Apps>();
            for (ResolveInfo resolveInfo : resolveInfoList) {
                appsList.add(new Apps(resolveInfo));
            }
            // create list to adapter
            ListAdapter listAdapter = new ArrayAdapter<Apps>(getActivity(), android.R.layout.select_dialog_item, android.R.id.text1, appsList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    // get dp
                    float dp = getResources().getDisplayMetrics().density;
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    // set size
                    int iconSize = (int) (40 * dp);
                    int viewSize = (int) (50 * dp);
                    int paddingSize = (int) (15 * dp);
                    // resize app icon (bitmap_factory makes low-quality images)
                    Drawable appIcon = appsList.get(position).icon;
                    appIcon.setBounds(0, 0, iconSize, iconSize);
                    // resize text size
                    textView.setTextSize(20);
                    // set app-icon and bounds
                    textView.setCompoundDrawables(appIcon, null, null, null);
                    textView.setCompoundDrawablePadding(paddingSize);
                    // set textView-height
                    textView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, viewSize));
                    return view;
                }
            };

            // make alert from list-adapter
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.intent_title))
                    .setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int item) {
                            // start activity
                            Apps apps = appsList.get(item);
                            intent.setClassName(apps.packageName, apps.className);
                            startActivity(intent);
                        }
                    });

            return builder.create();
        }

        class Apps {
            // class to list-adapter
            public final Drawable icon;
            public final String name;
            public final String packageName;
            public final String className;

            public Apps(ResolveInfo resolveInfo) {
                this.name = resolveInfo.loadLabel(getActivity().getPackageManager()).toString();
                this.icon = resolveInfo.loadIcon(getActivity().getPackageManager());
                this.packageName = resolveInfo.activityInfo.packageName;
                this.className = resolveInfo.activityInfo.name;
            }

            @Override
            public String toString() {
                // return name to list-adapter text
                return name;
            }
        }
    }

    public static class SaveDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // get site, file from bundle
            final Bundle bundle = getArguments();
            final String sitename = bundle.getString("sitename");
            final String filename = bundle.getString("filename");
            final String url = bundle.getString("file_url");
            // set directory
            final String directory = "PLViewer";
            final File root = Environment.getExternalStorageDirectory();
            final File dir = new File(root, directory + "/" + sitename);
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
                            try {
                                //save file
                                Uri uri = Uri.parse(url);
                                DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                DownloadManager.Request request = new DownloadManager.Request(uri);
                                request.setDestinationUri(Uri.fromFile(path));
                                request.setTitle("PhotoLinkViewer");
                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                                downloadManager.enqueue(request);
                                Toast.makeText(getActivity(), getString(R.string.download_photo_title) + path.toString(), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e("error", e.toString());
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.save_dialog_negative), null);
            return builder.create();
        }
    }

    class RotateRButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //get display size
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int dispwidth = size.x;
            int dispheight = size.y;
            ImageView imageView = (ImageView) getActivity().findViewById(R.id.imgview);
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            matrix.postRotate(90, dispwidth / 2, dispheight / 2);
            imageView.setImageMatrix(matrix);
        }

    }

    class RotateLButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //get display size
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int dispwidth = size.x;
            int dispheight = size.y;
            ImageView imageView = (ImageView) getActivity().findViewById(R.id.imgview);
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            matrix.postRotate(-90, dispwidth / 2, dispheight / 2);
            imageView.setImageMatrix(matrix);
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        baseButton.setImageBitmap(null);
        dlButton.setImageBitmap(null);
        setButton.setImageBitmap(null);
        webButton.setImageBitmap(null);
        rotateRButton.setImageBitmap(null);
        rotateLButton.setImageBitmap(null);
    }

}
