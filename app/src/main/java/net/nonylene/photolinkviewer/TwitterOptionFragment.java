package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import twitter4j.AsyncTwitter;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;

public class TwitterOptionFragment extends OptionFragment {
    private View view;
    private ImageButton retweet_button;
    private ImageButton favorite_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = super.onCreateView(inflater, container, savedInstanceState);
        final Bundle bundle = getArguments();
        // add twitter buttons
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.buttons);
        View twitterView = inflater.inflate(R.layout.twitter_option, linearLayout, false);
        linearLayout.addView(twitterView);
        retweet_button = (ImageButton) view.findViewById(R.id.retweet_button);
        retweet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RetweetDialogFragment dialogFragment = new RetweetDialogFragment();
                dialogFragment.setArguments(bundle);
                dialogFragment.show(getFragmentManager(), "retweet");
            }
        });
        favorite_button = (ImageButton) view.findViewById(R.id.favorite_button);
        favorite_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteDialogFragment favoriteDialogFragment = new FavoriteDialogFragment();
                favoriteDialogFragment.setArguments(bundle);
                favoriteDialogFragment.show(getFragmentManager(), "favorite");
            }
        });
        return view;
    }

    public static class FavoriteDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AsyncTwitter twitter = MyAsyncTwitter.getAsyncTwitter(getActivity().getApplicationContext());
            final Long id_long = getArguments().getLong("id_long");
            final Activity activity = getActivity();
            twitter.addListener(new TwitterAdapter() {
                @Override
                public void onException(TwitterException e, TwitterMethod twitterMethod) {
                    Log.e("twitterException", e.toString());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity.getApplicationContext(), getString(R.string.twitter_error_toast), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void createdFavorite(Status status) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity.getApplicationContext(), getString(R.string.toast_favorite), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

            SharedPreferences sharedPreferences  = activity.getSharedPreferences("preference", Context.MODE_PRIVATE);
            String screenName = sharedPreferences.getString("screen_name", null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.favorite_dialog_title))
                    .setMessage("@" + screenName + "\n" + getString(R.string.favorite_dialog_message))
                    .setPositiveButton(getString(R.string.favorite_dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            twitter.createFavorite(id_long);
                        }
                    })
                    .setNegativeButton(getString(R.string.favorite_dialog_ng), null);
            return builder.create();
        }

    }

    public static class RetweetDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AsyncTwitter twitter = (AsyncTwitter) getArguments().getSerializable("twitter");
            final Long id_long = getArguments().getLong("id_long");
            final Activity activity = getActivity();
            twitter.addListener(new TwitterAdapter() {
                @Override
                public void onException(TwitterException e, TwitterMethod twitterMethod) {
                    Log.e("twitterException", e.toString());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity.getApplicationContext(), getString(R.string.twitter_error_toast), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void retweetedStatus(Status status) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity.getApplicationContext(), getString(R.string.toast_retweet), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

            SharedPreferences sharedPreferences  = activity.getSharedPreferences("preference", Context.MODE_PRIVATE);
            String screenName = sharedPreferences.getString("screen_name", null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.retweet_dialog_title))
                    .setMessage("@" + screenName + "\n" + getString(R.string.retweet_dialog_message))
                    .setPositiveButton(getString(R.string.retweet_dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            twitter.retweetStatus(id_long);
                        }
                    })
                    .setNegativeButton(getString(R.string.retweet_dialog_ng), null);
            return builder.create();
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        retweet_button.setImageBitmap(null);
        favorite_button.setImageBitmap(null);
    }
}
