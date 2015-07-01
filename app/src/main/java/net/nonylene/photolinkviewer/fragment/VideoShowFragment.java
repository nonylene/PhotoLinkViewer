package net.nonylene.photolinkviewer.fragment;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.tool.PLVUrl;
import net.nonylene.photolinkviewer.tool.ProgressBarListener;

public class VideoShowFragment extends Fragment {
    private View view;
    private ImageView imageView;
    private FrameLayout videoShowFrameLayout;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.videoshow_fragment, container, false);
        imageView = (ImageView) view.findViewById(R.id.video_image);
        videoShowFrameLayout = (FrameLayout) view.findViewById(R.id.videoshowframe);
        progressBar = (ProgressBar) view.findViewById(R.id.show_progress);
        if (getArguments().getBoolean("single_frag", false)) {
            videoShowFrameLayout.setBackgroundResource(R.color.transparent);
            // do not hide progressbar! progressbar of activity will be displayed under videoView.
        }
        PLVUrl plvUrl = getArguments().getParcelable("plvurl");
        playVideo(plvUrl);
        return view;
    }

    class VideoOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                pauseStop();
                imageView.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            } else if (e.getAction() == MotionEvent.ACTION_DOWN) {
                imageView.setImageResource(R.drawable.ic_play_arrow_grey_48dp);
            }
            return true;
        }

        private void pauseStop() {
            // resume() doesn't work
            VideoView videoView = (VideoView) view.findViewById(R.id.videoview);
            if (videoView.isPlaying()) {
                imageView.setVisibility(View.VISIBLE);
                videoView.pause();
            } else {
                imageView.setVisibility(View.GONE);
                videoView.start();
            }
        }

    }

    private void playVideo(PLVUrl plvUrl) {
        // view video
        final VideoView videoView = (VideoView) view.findViewById(R.id.videoview);
        videoView.setVideoURI(Uri.parse(plvUrl.getDisplayUrl()));
        if ("vine".equals(plvUrl.getSiteName())) {
            // touch to stop
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //remove progressbar
                    removeProgressBar();
                    videoShowFrameLayout.setBackgroundColor(getResources().getColor(R.color.background));
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    videoView.setBackgroundColor(Color.TRANSPARENT);
                    if (preferences.getBoolean("video_play", true)) {
                        videoView.start();
                    } else {
                        imageView.setVisibility(View.VISIBLE);
                        videoView.seekTo(1);
                    }
                }
            });
            VideoOnTouchListener videoOnTouchListener = new VideoOnTouchListener();
            videoView.setOnTouchListener(videoOnTouchListener);

        } else {
            final MediaController mediaController = new MediaController(getActivity());
            videoView.setMediaController(mediaController);
            // touch to stop
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //remove progressbar
                    removeProgressBar();
                    videoShowFrameLayout.setBackgroundColor(getResources().getColor(R.color.background));
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    videoView.setBackgroundColor(Color.TRANSPARENT);
                    if (preferences.getBoolean("video_play", true)) {
                        mp.start();
                    } else {
                        mediaController.show();
                        mp.seekTo(1);
                    }
                }
            });

            videoView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mediaController.isShowing()) {
                            mediaController.hide();
                        } else {
                            mediaController.show();
                        }
                    }
                    return true;
                }
            });
        }

        // loop
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
    }

    private void removeProgressBar() {
        videoShowFrameLayout.removeView(progressBar);
        if (getActivity() instanceof ProgressBarListener)
            ((ProgressBarListener) getActivity()).hideProgressBar();
    }
}
