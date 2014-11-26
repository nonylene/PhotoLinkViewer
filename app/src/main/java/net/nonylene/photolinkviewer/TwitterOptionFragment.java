package net.nonylene.photolinkviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import twitter4j.AsyncTwitter;

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
            final AsyncTwitter twitter = (AsyncTwitter) getArguments().getSerializable("twitter");
            final Long id_long = getArguments().getLong("id_long");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.favorite_dialog_title))
                    .setMessage(getString(R.string.favorite_dialog_message))
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.retweet_dialog_title))
                    .setMessage(getString(R.string.retweet_dialog_message))
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
