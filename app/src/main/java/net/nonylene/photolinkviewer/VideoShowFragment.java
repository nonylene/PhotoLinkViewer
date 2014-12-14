package net.nonylene.photolinkviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoShowFragment extends Fragment {
    private View view;
    private ImageButton baseButton;
    private ImageButton setButton;
    private ImageButton webButton;
    private Boolean open = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.videoshow_fragment, container, false);
        baseButton = (ImageButton) view.findViewById(R.id.basebutton);
        setButton = (ImageButton) view.findViewById(R.id.setbutton);
        webButton = (ImageButton) view.findViewById(R.id.webbutton);
        baseButton.setOnClickListener(new BaseButtonClickListener());
        setButton.setOnClickListener(new SetButtonClickListener());
        webButton.setOnClickListener(new WebButtonClickListener());
        String url = getArguments().getString("url");
        URLPurser(url);
        return view;
    }

    class BaseButtonClickListener implements View.OnClickListener {

        public void onClick(View v) {
            if (open) {
                baseButton.setImageResource(R.drawable.up_button_design);
                setButton.setVisibility(View.GONE);
                webButton.setVisibility(View.GONE);
                open = false;
            } else {
                baseButton.setImageResource(R.drawable.down_button_design);
                setButton.setVisibility(View.VISIBLE);
                webButton.setVisibility(View.VISIBLE);
                open = true;
            }
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

    public class AsyncJSONExecute implements LoaderManager.LoaderCallbacks<JSONObject> {


        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<JSONObject> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                return new AsyncJSON(getActivity().getApplicationContext(), url);
            } catch (IOException e) {
                Log.e("JSONLoaderError", e.toString());
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<JSONObject> loader, JSONObject json) {

            try {
                Log.v("json", json.toString(2));
                JSONObject data = json.getJSONObject("data");
                JSONObject records = data.getJSONArray("records").getJSONObject(0);
                String file = records.getString("videoUrl");
                Log.v("URL", file);
                // view video
                final VideoView videoView = (VideoView) view.findViewById(R.id.videoview);
                videoView.setVideoURI(Uri.parse(file));
                // touch to stop
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        //remove progressbar
                        FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.videoshowframe);
                        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.videoshowprogress);
                        frameLayout.removeView(progressBar);
                        videoView.start();
                    }
                });
                // loop
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        videoView.start();
                    }
                });
                VideoOnTouchListener videoOnTouchListener = new VideoOnTouchListener();
                videoView.setOnTouchListener(videoOnTouchListener);
            } catch (Exception e) {
                Log.e("JSONError", e.toString());
            }
        }

        @Override
        public void onLoaderReset(Loader<JSONObject> loader) {

        }
    }


    class VideoOnTouchListener implements View.OnTouchListener {
        int position = 0;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                // resume() doesn't work
                VideoView videoView = (VideoView) view.findViewById(R.id.videoview);
                if (videoView.isPlaying()) {
                    position = videoView.getCurrentPosition();
                    videoView.pause();
                } else {
                    videoView.seekTo(position);
                    videoView.start();
                }
            }
            return true;
        }
    }


    public void URLPurser(String url) {
        try {
            Log.v("vine", url);
            Pattern pattern = Pattern.compile("^https?://vine\\.co/v/(\\w+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                Log.v("match", "success");
            }
            String id = Base49.decode(matcher.group(1));
            Log.v("Vine", id);
            String request = "https://api.vineapp.com/timelines/posts/" + id;
            Log.v("vineAPI", request);
            AsyncJSONExecute hoge = new AsyncJSONExecute();
            hoge.Start(request);
        } catch (
                Exception e
                ) {
            Log.e("IOException", e.toString());
        }
    }
}
