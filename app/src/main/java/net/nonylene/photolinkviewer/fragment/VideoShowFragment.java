package net.nonylene.photolinkviewer.fragment;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.tool.Base49;
import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.tool.MyJsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoShowFragment extends Fragment {
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.videoshow_fragment, container, false);
        String url = getArguments().getString("url");
        URLPurser(url);
        return view;
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
            String request = "https://api.vineapp.com/timelines/posts/" + id;
            // volley
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            queue.add(new MyJsonObjectRequest(getActivity(), request,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            parseVine(response);
                        }
                    }
            ));
        } catch (
                Exception e
                ) {
            Log.e("IOException", e.toString());
        }
    }

    private void parseVine(JSONObject json){
        final FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.videoshowframe);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.videoshowprogress);
        if (json != null) {
            try {
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
                        frameLayout.removeView(progressBar);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        if (preferences.getBoolean("video_play",true)){
                            videoView.start();
                        }else {
                            videoView.seekTo(1);
                        }
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
            } catch (JSONException e) {
                Log.e("JSONError", e.toString());
                frameLayout.removeView(progressBar);
                Toast.makeText(getActivity(), getString(R.string.vine_json_toast), Toast.LENGTH_LONG).show();
            }
        } else {
            frameLayout.removeView(progressBar);
            Toast.makeText(getActivity(), getString(R.string.vine_url_toast), Toast.LENGTH_LONG).show();
        }
    }
}
